package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.coursework3.dto.request.ConfirmBookingPaymentRequest;
import org.example.coursework3.dto.request.CreateBookingPaymentRequest;
import org.example.coursework3.dto.request.CreateBookingRequest;
import org.example.coursework3.dto.response.BookingActionResult;
import org.example.coursework3.dto.response.BookingPageResult;
import org.example.coursework3.dto.response.ConfirmBookingPaymentResult;
import org.example.coursework3.dto.response.CreateBookingPaymentResult;
import org.example.coursework3.dto.response.CreateBookingResult;
import org.example.coursework3.entity.*;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.BookingHistoryRepository;
import org.example.coursework3.repository.BookingRepository;
import org.example.coursework3.repository.SlotRepository;
import org.example.coursework3.repository.UserRepository;
import org.example.coursework3.vo.MyBookingVo;
import org.example.coursework3.vo.SingleBookingVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerBookingService {
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final AliyunMailService aliyunMailService;
    private final AlipayGatewayService alipayGatewayService;
    private final BookingHistoryRepository bookingHistoryRepository;
    private static final String PAYMENT_DRAFT_KEY = "booking:payment:draft:";
    private static final String OUT_TRADE_KEY = "booking:payment:outTrade:";
    private final RedisTemplate<String, Object> redisTemplate;
//    private final Map<String, PaymentDraft> bookingPaymentDrafts = new ConcurrentHashMap<>();
//    private final Map<String, String> bookingIdByOutTradeNo = new ConcurrentHashMap<>();


    @Transactional
    public CreateBookingResult creatBooking(String userId, CreateBookingRequest request) {
        Slot slot = slotRepository.getById(request.getSlotId());
        if (!slot.getAvailable()){
            throw new MsgException("请选择有效时段");
        }
        Booking booking = new Booking();
        booking.setCustomerId(userId);
        booking.setSlotId(request.getSlotId());
        booking.setSpecialistId(request.getSpecialistId());
        booking.setNote(request.getNote());
        bookingRepository.save(booking);
        slot.setAvailable(false);
        slotRepository.save(slot);

        return new CreateBookingResult(booking.getId(), booking.getSpecialistId(), booking.getSlotId(), booking.getStatus());
    }

    public CreateBookingPaymentResult createBookingPayment(String userId, String bookingId, CreateBookingPaymentRequest request) {
        Booking booking = getOwnedBooking(userId, bookingId);
        Slot slot = slotRepository.findById(booking.getSlotId())
                .orElseThrow(() -> new MsgException("预约时段不存在"));

        double amount = resolvePaymentAmount(request, slot);
        String currency = resolveCurrency(request, slot);
        String normalizedAmount = String.format(Locale.US, "%.2f", amount);
        String paymentToken = buildPaymentToken(booking.getId());
        String subject = "Booking " + booking.getId();
        String alipayQrRawCode = alipayGatewayService.precreate(paymentToken, normalizedAmount, subject);
        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=280x280&data="
                + URLEncoder.encode(alipayQrRawCode, StandardCharsets.UTF_8);

//        bookingPaymentDrafts.put(booking.getId(), new PaymentDraft(paymentToken, paymentToken, amount, currency, userId));
        redisTemplate.opsForValue().set(
                PAYMENT_DRAFT_KEY + booking.getId(),
                new PaymentDraft(paymentToken, paymentToken, amount, currency, userId),
                Duration.ofMinutes(15));
//        bookingIdByOutTradeNo.put(paymentToken, booking.getId());
        redisTemplate.opsForValue().set(
                OUT_TRADE_KEY + paymentToken,
                booking.getId(),
                Duration.ofMinutes(15)
        );
        log.info("create outTradeNo: {}", paymentToken);
        return new CreateBookingPaymentResult(paymentToken, paymentToken, qrCodeUrl, amount, currency);
    }

    public ConfirmBookingPaymentResult confirmBookingPayment(String userId, String bookingId, ConfirmBookingPaymentRequest request) {
        Booking booking = getOwnedBooking(userId, bookingId);
//        PaymentDraft draft = bookingPaymentDrafts.get(booking.getId());
        PaymentDraft draft = (PaymentDraft) redisTemplate.opsForValue()
                .get(PAYMENT_DRAFT_KEY + booking.getId());
        if (draft == null) {
            throw new MsgException("请先创建支付单");
        }
        if (!draft.getCustomerId().equals(userId)) {
            throw new MsgException("无权限操作该支付单");
        }
        String paymentId = safeTrim(request == null ? null : request.getPaymentId());
        if (!paymentId.isBlank() && !draft.getPaymentId().equals(paymentId)) {
            throw new MsgException("支付单不匹配");
        }

        if (draft.isPaid()) {
            return new ConfirmBookingPaymentResult(booking.getId(), draft.getPaymentId(), "SUCCESS", booking.getStatus());
        }

        log.info("query outTradeNo: {}", draft.getOutTradeNo());

        String tradeStatus = alipayGatewayService.queryTradeStatus(draft.getOutTradeNo());
        if (!isAlipaySuccess(tradeStatus)) {
            throw new MsgException("支付未完成，当前状态: " + safeTrim(tradeStatus));
        }

        draft.setPaid(true);
        redisTemplate.opsForValue().set(
                PAYMENT_DRAFT_KEY + booking.getId(),
                draft,
                Duration.ofMinutes(15)
        );
        return new ConfirmBookingPaymentResult(booking.getId(), draft.getPaymentId(), "SUCCESS", booking.getStatus());
    }

    public boolean handleAlipayNotify(Map<String, String> notifyParams) {
        if (notifyParams == null || notifyParams.isEmpty()) {
            return false;
        }
        boolean verified = alipayGatewayService.verifyNotify(notifyParams);
        if (!verified) {
            return false;
        }
        String outTradeNo = safeTrim(notifyParams.get("out_trade_no"));
        String tradeStatus = safeTrim(notifyParams.get("trade_status"));
        if (!isAlipaySuccess(tradeStatus)) {
            return false;
        }
//        String bookingId = bookingIdByOutTradeNo.get(outTradeNo);
        String bookingId = (String) redisTemplate.opsForValue()
                .get(OUT_TRADE_KEY + outTradeNo);
        if (bookingId == null) {
            return false;
        }
        PaymentDraft draft = (PaymentDraft) redisTemplate.opsForValue()
                .get(PAYMENT_DRAFT_KEY + bookingId);
        if (draft != null) {
            draft.setPaid(true);
            redisTemplate.opsForValue().set(
                    PAYMENT_DRAFT_KEY + bookingId,
                    draft,
                    Duration.ofMinutes(15)
            );
        }
        return true;
    }

    public BookingPageResult getMyBookings(String userId, String status, Integer page, Integer pageSize, String from, String to) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;

        List<Booking> bookings;
        BookingStatus bookingStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                bookingStatus = BookingStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new MsgException("无效的状态值：" + status);
            }
        }

        if (bookingStatus == null) {
            bookings = bookingRepository.findByCustomerId(userId);
        } else {
            bookings = bookingRepository.findByCustomerIdAndStatus(userId, bookingStatus);
        }

        LocalDateTime fromTime = parseDate(from, true);
        LocalDateTime toTime = parseDate(to, false);
        List<MyBookingVo> allItems = new ArrayList<>();

        bookings.stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
                .forEach(booking -> {
                    Slot slot = slotRepository.findById(booking.getSlotId()).orElse(null);
                    if (slot == null) {
                        return;
                    }
                    LocalDateTime startTime = slot.getStartTime();
                    if (fromTime != null && startTime.isBefore(fromTime)) {
                        return;
                    }
                    if (toTime != null && startTime.isAfter(toTime)) {
                        return;
                    }
                    User specialist = userRepository.findById(booking.getSpecialistId());
                    String specialistName = specialist != null ? specialist.getName() : booking.getSpecialistId();
                    allItems.add(MyBookingVo.fromBooking(booking, slot, specialistName));
                });

        int total = allItems.size();
        int start = Math.min((safePage - 1) * safePageSize, total);
        int end = Math.min(start + safePageSize, total);
        List<MyBookingVo> pageItems = allItems.subList(start, end);

        return BookingPageResult.of(pageItems, total, safePage, safePageSize);
    }

    private LocalDateTime parseDate(String value, boolean isFrom) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDate date = LocalDate.parse(value);
            return isFrom ? date.atStartOfDay() : date.atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            throw new MsgException("日期格式错误：" + value);
        }
    }

    public SingleBookingVo getSingleBookingInfo(String bookingId){
        Booking booking = bookingRepository.getBookingById(bookingId);
        Slot slot = slotRepository.getSlotById(booking.getSlotId());
        User specialist = userRepository.findById(booking.getSpecialistId());
        String specialistName = specialist != null ? specialist.getName() : booking.getSpecialistId();
        String customerName = setNameInfo(booking.getCustomerId());
        return SingleBookingVo.fromBooking(booking, slot, specialistName ,customerName);
    }

    public String setNameInfo(String userId){
        User user = userRepository.getUserById(userId);
        return user.getName();
    }

    private Booking getOwnedBooking(String userId, String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new MsgException("预约不存在"));
        if (!userId.equals(booking.getCustomerId())) {
            throw new MsgException("无权限操作该预约");
        }
        return booking;
    }

    private double resolvePaymentAmount(CreateBookingPaymentRequest request, Slot slot) {
        double fallback = slot.getAmount() == null ? 0.0 : slot.getAmount().doubleValue();
        if (request == null || request.getAmount() == null) {
            return fallback;
        }
        return request.getAmount() < 0 ? fallback : request.getAmount();
    }

    private String resolveCurrency(CreateBookingPaymentRequest request, Slot slot) {
        String fromSlot = safeTrim(slot.getCurrency());
        if (!fromSlot.isBlank()) {
            return fromSlot;
        }
        String fromRequest = safeTrim(request == null ? null : request.getCurrency());
        if (!fromRequest.isBlank()) {
            return fromRequest;
        }
        return "CNY";
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isAlipaySuccess(String status) {
        return "TRADE_SUCCESS".equalsIgnoreCase(status) || "TRADE_FINISHED".equalsIgnoreCase(status);
    }

    private String buildPaymentToken(String bookingId) {
        long now = System.currentTimeMillis();
        String compactId = bookingId == null ? "booking" : bookingId.replace("-", "");
        if (compactId.length() > 20) {
            compactId = compactId.substring(0, 20);
        }
        return "BK" + now + compactId;
    }

    @Transactional
    public BookingActionResult cancelBooking(String id) {
        Booking booking = bookingRepository.getBookingById(id);
        if (booking.getStatus() != BookingStatus.Confirmed && booking.getStatus() != BookingStatus.Pending) {
            throw new MsgException("当前预约状态无法执行取消操作");
        }
        booking.setStatus(BookingStatus.Cancelled);
        bookingRepository.save(booking);
        Slot slot = slotRepository.getSlotById(booking.getSlotId());
        slot.setAvailable(true);

        return new BookingActionResult(id, BookingStatus.Cancelled);
    }

    @Transactional
    public void rescheduleBooking(String bookingId, String newSlotId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new MsgException("预约不存在"));

        if (booking.getStatus() == BookingStatus.Cancelled || booking.getStatus() == BookingStatus.Completed) {
            throw new MsgException("该预约无法改期");
        }

        Slot newSlot = slotRepository.findById(newSlotId)
                .orElseThrow(() -> new MsgException("新时段不存在"));
        if (!newSlot.getAvailable()) {
            throw new MsgException("新时段不可用");
        }
        if (!newSlot.getSpecialistId().equals(booking.getSpecialistId())) {
            throw new MsgException("新时段与原专家不匹配");
        }

        BookingHistory history = new BookingHistory();
        history.setBookingId(bookingId);
        history.setStatus(BookingStatus.Rescheduled);
        bookingHistoryRepository.save(history);

        Slot oldSlot = slotRepository.findById(booking.getSlotId()).orElse(null);
        if (oldSlot != null) {
            oldSlot.setAvailable(true);
            slotRepository.save(oldSlot);
        }
        booking.setSlotId(newSlotId);
        booking.setStatus(BookingStatus.Pending);
        bookingRepository.save(booking);
        newSlot.setAvailable(false);
        slotRepository.save(newSlot);



        try {
            User customer = userRepository.findById(booking.getCustomerId());
            User specialistUser = userRepository.findById(booking.getSpecialistId());

            String range = newSlot.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " to " +
                    newSlot.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            if (customer != null && customer.getEmail() != null) {
                aliyunMailService.sendGenericStatusNotification(customer.getEmail(), "Customer", "Rescheduled", range, "Your booking has been rescheduled to a new time.");
            }
            if (specialistUser != null && specialistUser.getEmail() != null) {
                aliyunMailService.sendGenericStatusNotification(specialistUser.getEmail(), "Specialist", "Rescheduled", range, "Customer rescheduled the booking to a new time.");
            }
        } catch (Exception e) {
            log.warn("发送改期通知邮件失败: {}", e.getMessage());
        }
    }



}

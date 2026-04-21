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
import org.example.coursework3.dto.response.UnpaidPaymentItemResult;
import org.example.coursework3.dto.response.UnpaidPaymentsResult;
import org.example.coursework3.entity.*;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.BookingHistoryRepository;
import org.example.coursework3.repository.BookingRepository;
import org.example.coursework3.repository.SlotRepository;
import org.example.coursework3.repository.UserRepository;
import org.example.coursework3.vo.MyBookingVo;
import org.example.coursework3.vo.SingleBookingVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


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
    private static final String USER_UNPAID_KEY = "booking:payment:user:";
    private static final Duration PAYMENT_TTL = Duration.ofMinutes(15);
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
    private final RedisTemplate<String, Object> redisTemplate;
    @Value("${payment.mock-enabled:false}")
    private boolean mockPaymentEnabled;
//    private final Map<String, PaymentDraft> bookingPaymentDrafts = new ConcurrentHashMap<>();
//    private final Map<String, String> bookingIdByOutTradeNo = new ConcurrentHashMap<>();


    @Transactional
    public CreateBookingResult creatBooking(String userId, CreateBookingRequest request) {
        String paymentId = safeTrim(request.getPaymentId());
        if (paymentId.isBlank()) {
            throw new MsgException("请先完成支付");
        }
        String paymentIntentId = (String) redisTemplate.opsForValue().get(OUT_TRADE_KEY + paymentId);
        if (paymentIntentId == null) {
            throw new MsgException("支付单无效或已过期，请重新支付");
        }
        PaymentDraft draft = (PaymentDraft) redisTemplate.opsForValue().get(PAYMENT_DRAFT_KEY + paymentIntentId);
        if (draft == null) {
            throw new MsgException("支付信息无效或已过期，请重新支付");
        }
        if (!userId.equals(draft.getCustomerId())) {
            throw new MsgException("无权限创建该预约");
        }
        if (!draft.isPaid()) {
            throw new MsgException("支付尚未完成，请完成支付后再提交预约");
        }
        if (!safeTrim(request.getSpecialistId()).equals(safeTrim(draft.getSpecialistId()))
                || !safeTrim(request.getSlotId()).equals(safeTrim(draft.getSlotId()))) {
            throw new MsgException("支付信息与预约信息不一致，请重新支付");
        }

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
        redisTemplate.delete(PAYMENT_DRAFT_KEY + paymentIntentId);
        redisTemplate.delete(OUT_TRADE_KEY + paymentId);
        redisTemplate.opsForZSet().remove(userUnpaidKey(userId), paymentIntentId);

        return new CreateBookingResult(booking.getId(), booking.getSpecialistId(), booking.getSlotId(), booking.getStatus());
    }

    public CreateBookingPaymentResult createBookingPayment(String userId, String paymentIntentId, CreateBookingPaymentRequest request) {
        String specialistId = safeTrim(request == null ? null : request.getSpecialistId());
        String slotId = safeTrim(request == null ? null : request.getSlotId());
        if (specialistId.isBlank() || slotId.isBlank()) {
            throw new MsgException("specialistId和slotId不能为空");
        }
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new MsgException("预约时段不存在"));
        if (!specialistId.equals(slot.getSpecialistId())) {
            throw new MsgException("专家与时段不匹配");
        }
        if (!Boolean.TRUE.equals(slot.getAvailable())) {
            throw new MsgException("该时段已被占用，请选择其他时段");
        }
        String normalizedIntentId = safeTrim(paymentIntentId);
        if (normalizedIntentId.isBlank()) {
            throw new MsgException("payment intent id不能为空");
        }

        double amount = resolvePaymentAmount(request, slot);
        String currency = resolveCurrency(request, slot);
        String normalizedAmount = String.format(Locale.US, "%.2f", amount);
        String paymentToken = buildPaymentToken(normalizedIntentId);
        String subject = "Booking " + normalizedIntentId;
        String alipayQrRawCode = alipayGatewayService.precreate(paymentToken, normalizedAmount, subject);
        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=280x280&data="
                + URLEncoder.encode(alipayQrRawCode, StandardCharsets.UTF_8);

//        bookingPaymentDrafts.put(booking.getId(), new PaymentDraft(paymentToken, paymentToken, amount, currency, userId));
        redisTemplate.opsForValue().set(
                PAYMENT_DRAFT_KEY + normalizedIntentId,
                new PaymentDraft(paymentToken, paymentToken, amount, currency, userId, specialistId, slotId),
                PAYMENT_TTL);
//        bookingIdByOutTradeNo.put(paymentToken, booking.getId());
        redisTemplate.opsForValue().set(
                OUT_TRADE_KEY + paymentToken,
                normalizedIntentId,
                PAYMENT_TTL
        );
        long expiresAtMs = Instant.now().plus(PAYMENT_TTL).toEpochMilli();
        redisTemplate.opsForZSet().add(userUnpaidKey(userId), normalizedIntentId, expiresAtMs);
        log.info("create outTradeNo: {}", paymentToken);
        return new CreateBookingPaymentResult(paymentToken, paymentToken, qrCodeUrl, amount, currency);
    }

    public ConfirmBookingPaymentResult confirmBookingPayment(String userId, String paymentIntentId, ConfirmBookingPaymentRequest request) {
//        PaymentDraft draft = bookingPaymentDrafts.get(booking.getId());
        PaymentDraft draft = (PaymentDraft) redisTemplate.opsForValue()
                .get(PAYMENT_DRAFT_KEY + paymentIntentId);
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
            return new ConfirmBookingPaymentResult(paymentIntentId, draft.getPaymentId(), "SUCCESS", BookingStatus.Pending);
        }

        log.info("query outTradeNo: {}", draft.getOutTradeNo());

        String tradeStatus = alipayGatewayService.queryTradeStatus(draft.getOutTradeNo());
        if (!isAlipaySuccess(tradeStatus)) {
            throw new MsgException("支付未完成，当前状态: " + safeTrim(tradeStatus));
        }

        draft.setPaid(true);
        redisTemplate.opsForValue().set(
                PAYMENT_DRAFT_KEY + paymentIntentId,
                draft,
                PAYMENT_TTL
        );
        redisTemplate.opsForZSet().remove(userUnpaidKey(userId), paymentIntentId);
        return new ConfirmBookingPaymentResult(paymentIntentId, draft.getPaymentId(), "SUCCESS", BookingStatus.Pending);
    }

    public ConfirmBookingPaymentResult mockSuccessPayment(String userId, String paymentIntentId) {
        if (!mockPaymentEnabled) {
            throw new MsgException("Mock支付未开启");
        }
        String normalizedIntentId = safeTrim(paymentIntentId);
        if (normalizedIntentId.isBlank()) {
            throw new MsgException("payment intent id不能为空");
        }
        PaymentDraft draft = (PaymentDraft) redisTemplate.opsForValue().get(PAYMENT_DRAFT_KEY + normalizedIntentId);
        if (draft == null) {
            throw new MsgException("请先创建支付单");
        }
        if (!userId.equals(draft.getCustomerId())) {
            throw new MsgException("无权限操作该支付单");
        }
        if (!draft.isPaid()) {
            draft.setPaid(true);
            redisTemplate.opsForValue().set(PAYMENT_DRAFT_KEY + normalizedIntentId, draft, PAYMENT_TTL);
            redisTemplate.opsForZSet().remove(userUnpaidKey(userId), normalizedIntentId);
        }
        return new ConfirmBookingPaymentResult(normalizedIntentId, draft.getPaymentId(), "SUCCESS", BookingStatus.Pending);
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
//        String paymentIntentId = bookingIdByOutTradeNo.get(outTradeNo);
        String paymentIntentId = (String) redisTemplate.opsForValue()
                .get(OUT_TRADE_KEY + outTradeNo);
        if (paymentIntentId == null) {
            return false;
        }
        PaymentDraft draft = (PaymentDraft) redisTemplate.opsForValue()
                .get(PAYMENT_DRAFT_KEY + paymentIntentId);
        if (draft != null) {
            draft.setPaid(true);
            redisTemplate.opsForValue().set(
                    PAYMENT_DRAFT_KEY + paymentIntentId,
                    draft,
                    PAYMENT_TTL
            );
            redisTemplate.opsForZSet().remove(userUnpaidKey(draft.getCustomerId()), paymentIntentId);
        }
        return true;
    }

    public UnpaidPaymentsResult listUnpaidPayments(String userId) {
        Set<Object> intentSet = redisTemplate.opsForZSet().range(userUnpaidKey(userId), 0, -1);
        List<UnpaidPaymentItemResult> items = new ArrayList<>();
        if (intentSet == null || intentSet.isEmpty()) {
            return new UnpaidPaymentsResult(items);
        }
        long nowMs = Instant.now().toEpochMilli();
        for (Object raw : intentSet) {
            String intentId = safeTrim(raw == null ? null : String.valueOf(raw));
            if (intentId.isBlank()) continue;
            UnpaidPaymentItemResult item = loadUnpaidItem(userId, intentId, nowMs, true);
            if (item != null) {
                items.add(item);
            }
        }
        items.sort((a, b) -> Long.compare(b.getRemainingSeconds() == null ? 0 : b.getRemainingSeconds(),
                a.getRemainingSeconds() == null ? 0 : a.getRemainingSeconds()));
        return new UnpaidPaymentsResult(items);
    }

    public UnpaidPaymentItemResult getUnpaidPayment(String userId, String paymentIntentId) {
        return loadUnpaidItem(userId, paymentIntentId, Instant.now().toEpochMilli(), false);
    }

    public CreateBookingPaymentResult resumeUnpaidPayment(String userId, String paymentIntentId) {
        String normalizedIntentId = safeTrim(paymentIntentId);
        if (normalizedIntentId.isBlank()) {
            throw new MsgException("payment intent id不能为空");
        }
        PaymentDraft draft = (PaymentDraft) redisTemplate.opsForValue().get(PAYMENT_DRAFT_KEY + normalizedIntentId);
        if (draft == null) {
            redisTemplate.opsForZSet().remove(userUnpaidKey(userId), normalizedIntentId);
            throw new MsgException("未支付订单不存在或已过期");
        }
        if (!userId.equals(draft.getCustomerId())) {
            throw new MsgException("无权限操作该支付单");
        }
        if (draft.isPaid()) {
            redisTemplate.opsForZSet().remove(userUnpaidKey(userId), normalizedIntentId);
            throw new MsgException("该支付单已完成支付");
        }
        Slot slot = slotRepository.findById(draft.getSlotId())
                .orElseThrow(() -> new MsgException("预约时段不存在"));
        if (!Boolean.TRUE.equals(slot.getAvailable())) {
            cleanupIntent(draft.getCustomerId(), normalizedIntentId, draft.getPaymentId());
            throw new MsgException("该时段已被占用，请重新选择");
        }
        if (!safeTrim(slot.getSpecialistId()).equals(safeTrim(draft.getSpecialistId()))) {
            cleanupIntent(draft.getCustomerId(), normalizedIntentId, draft.getPaymentId());
            throw new MsgException("专家与时段不匹配");
        }

        String newPaymentToken = buildPaymentToken(normalizedIntentId);
        String normalizedAmount = String.format(Locale.US, "%.2f", draft.getAmount() == null ? 0 : draft.getAmount());
        String subject = "Booking " + normalizedIntentId;
        String alipayQrRawCode = alipayGatewayService.precreate(newPaymentToken, normalizedAmount, subject);
        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=280x280&data="
                + URLEncoder.encode(alipayQrRawCode, StandardCharsets.UTF_8);

        String oldPaymentId = safeTrim(draft.getPaymentId());
        if (!oldPaymentId.isBlank()) {
            redisTemplate.delete(OUT_TRADE_KEY + oldPaymentId);
        }
        draft.setOutTradeNo(newPaymentToken);
        draft.setPaymentId(newPaymentToken);
        draft.setPaid(false);
        redisTemplate.opsForValue().set(PAYMENT_DRAFT_KEY + normalizedIntentId, draft, PAYMENT_TTL);
        redisTemplate.opsForValue().set(OUT_TRADE_KEY + newPaymentToken, normalizedIntentId, PAYMENT_TTL);
        long expiresAtMs = Instant.now().plus(PAYMENT_TTL).toEpochMilli();
        redisTemplate.opsForZSet().add(userUnpaidKey(userId), normalizedIntentId, expiresAtMs);
        return new CreateBookingPaymentResult(newPaymentToken, newPaymentToken, qrCodeUrl, draft.getAmount(), draft.getCurrency());
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

    private String userUnpaidKey(String userId) {
        return USER_UNPAID_KEY + userId;
    }

    private void cleanupIntent(String userId, String intentId, String paymentId) {
        redisTemplate.delete(PAYMENT_DRAFT_KEY + intentId);
        if (!safeTrim(paymentId).isBlank()) {
            redisTemplate.delete(OUT_TRADE_KEY + paymentId);
        }
        redisTemplate.opsForZSet().remove(userUnpaidKey(userId), intentId);
    }

    private UnpaidPaymentItemResult loadUnpaidItem(String userId, String paymentIntentId, long nowMs, boolean silent) {
        String intentId = safeTrim(paymentIntentId);
        if (intentId.isBlank()) return null;
        PaymentDraft draft = (PaymentDraft) redisTemplate.opsForValue().get(PAYMENT_DRAFT_KEY + intentId);
        if (draft == null) {
            redisTemplate.opsForZSet().remove(userUnpaidKey(userId), intentId);
            return null;
        }
        if (!userId.equals(draft.getCustomerId())) {
            if (silent) return null;
            throw new MsgException("无权限查看该支付单");
        }
        if (draft.isPaid()) {
            redisTemplate.opsForZSet().remove(userUnpaidKey(userId), intentId);
            return null;
        }
        Double score = redisTemplate.opsForZSet().score(userUnpaidKey(userId), intentId);
        long expiresAtMs = score == null ? nowMs : score.longValue();
        long remainSec = Math.max(0, (expiresAtMs - nowMs) / 1000);
        if (remainSec <= 0) {
            cleanupIntent(userId, intentId, draft.getPaymentId());
            return null;
        }
        Slot slot = slotRepository.findById(draft.getSlotId()).orElse(null);
        String slotLabel = draft.getSlotId();
        if (slot != null) {
            slotLabel = slot.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " - "
                    + slot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        String expiresAt = Instant.ofEpochMilli(expiresAtMs).atZone(APP_ZONE).toOffsetDateTime().toString();
        return new UnpaidPaymentItemResult(
                intentId,
                draft.getPaymentId(),
                draft.getSpecialistId(),
                draft.getSlotId(),
                slotLabel,
                draft.getAmount(),
                draft.getCurrency(),
                "UNPAID",
                expiresAt,
                remainSec
        );
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
        // get booking details by id
        Booking booking = bookingRepository.getBookingById(id);
        // verify: only bookings in 'Confirmed' or 'Pending' are eligible for cancellation
        if (booking.getStatus() != BookingStatus.Confirmed && booking.getStatus() != BookingStatus.Pending) {
            throw new MsgException("当前预约状态无法执行取消操作");
        }
        // update booking status to 'cancelled'
        booking.setStatus(BookingStatus.Cancelled);
        bookingRepository.save(booking);
        // get the cancelled slot and set it available
        Slot slot = slotRepository.getSlotById(booking.getSlotId());
        slot.setAvailable(true);

        return new BookingActionResult(id, BookingStatus.Cancelled);
    }

    @Transactional
    public void rescheduleBooking(String bookingId, String newSlotId) {
        // get booking details
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new MsgException("预约不存在"));
        // check if the current booking status allows rescheduling
        if (booking.getStatus() == BookingStatus.Cancelled || booking.getStatus() == BookingStatus.Completed) {
            throw new MsgException("该预约无法改期");
        }
        // validate new slot existence and availability
        Slot newSlot = slotRepository.findById(newSlotId)
                .orElseThrow(() -> new MsgException("新时段不存在"));
        if (!newSlot.getAvailable()) {
            throw new MsgException("新时段不可用");
        }
        // ensure the new slot belongs to the same specialist
        if (!newSlot.getSpecialistId().equals(booking.getSpecialistId())) {
            throw new MsgException("新时段与原专家不匹配");
        }
        // record the rescheduling action in the history database
        BookingHistory history = new BookingHistory();
        history.setBookingId(bookingId);
        history.setStatus(BookingStatus.Rescheduled);
        bookingHistoryRepository.save(history);
        // release the old time slot
        Slot oldSlot = slotRepository.findById(booking.getSlotId()).orElse(null);
        if (oldSlot != null) {
            oldSlot.setAvailable(true);
            slotRepository.save(oldSlot);
        }

        // update the booking with new slot and reset status to 'Pending'
        booking.setSlotId(newSlotId);
        booking.setStatus(BookingStatus.Pending);
        bookingRepository.save(booking);

        // lock the new time slot
        newSlot.setAvailable(false);
        slotRepository.save(newSlot);


        // send notifications to both parties
        try {
            User customer = userRepository.findById(booking.getCustomerId());
            User specialistUser = userRepository.findById(booking.getSpecialistId());
            // change format of time range for email
            String range = newSlot.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " to " +
                    newSlot.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            // send to customer
            if (customer != null && customer.getEmail() != null) {
                aliyunMailService.sendGenericStatusNotification(customer.getEmail(), "Customer", "Rescheduled", range, "Your booking has been rescheduled to a new time.");
            }
            // send to specialist
            if (specialistUser != null && specialistUser.getEmail() != null) {
                aliyunMailService.sendGenericStatusNotification(specialistUser.getEmail(), "Specialist", "Rescheduled", range, "Customer rescheduled the booking to a new time.");
            }
        } catch (Exception e) {
            log.warn("发送改期通知邮件失败: {}", e.getMessage());
        }
    }



}

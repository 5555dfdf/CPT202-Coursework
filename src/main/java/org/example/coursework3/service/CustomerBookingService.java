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

    /**
     * Finalizes the booking after a successful payment.
     * Validates that the payment draft exists, is paid, and matches the requested slot/specialist.
     *
     * @return CreateBookingResult containing the new booking ID and status.
     */
    @Transactional
    public CreateBookingResult creatBooking(String userId, CreateBookingRequest request) {
        String paymentId = safeTrim(request.getPaymentId());
        if (paymentId.isBlank()) {
            throw new MsgException("Please complete payment first");
        }
        // Verify payment validity via Redis mappings
        String paymentIntentId = (String) redisTemplate.opsForValue().get(OUT_TRADE_KEY + paymentId);
        if (paymentIntentId == null) {
            throw new MsgException("Payment expired! Try again!");
        }
        PaymentDraft draft = (PaymentDraft) redisTemplate.opsForValue().get(PAYMENT_DRAFT_KEY + paymentIntentId);
        if (draft == null) {
            throw new MsgException("Payment expired! Try again!");
        }
        if (!userId.equals(draft.getCustomerId())) {
            throw new MsgException("No right!");
        }
        if (!draft.isPaid()) {
            throw new MsgException("Please complete payment first");
        }
        // Validate requested details against the paid draft
        if (!safeTrim(request.getSpecialistId()).equals(safeTrim(draft.getSpecialistId()))
                || !safeTrim(request.getSlotId()).equals(safeTrim(draft.getSlotId()))) {
            throw new MsgException("Invalid Payment Info");
        }
        // Persist booking and update slot availability
        Slot slot = slotRepository.getById(request.getSlotId());
        if (!slot.getAvailable()){
            throw new MsgException("Please choose a valid slot");
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
    /**
     * Initializes a payment intent and generates an Alipay QR code.
     * Uses Redis to store a 'PaymentDraft' to prevent double-booking during the 15-minute payment window.
     */
    public CreateBookingPaymentResult createBookingPayment(String userId, String paymentIntentId, CreateBookingPaymentRequest request) {
        // Prevent duplicate unpaid orders for the same slot
        List<UnpaidPaymentItemResult> items = listUnpaidPayments(userId).getItems();
        for (UnpaidPaymentItemResult result : items){
            if (result.getSlotId().equals(request.getSlotId())){
                throw new MsgException("You have an untreated order for the same time period.");
            }
        }
        // Validate Slot and Specialist
        String specialistId = safeTrim(request == null ? null : request.getSpecialistId());
        String slotId = safeTrim(request == null ? null : request.getSlotId());
        if (specialistId.isBlank() || slotId.isBlank()) {
            throw new MsgException("specialistId and slotId cannot be empty");
        }
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new MsgException("The appointment time slot does not exist"));
        if (!specialistId.equals(slot.getSpecialistId())) {
            throw new MsgException("The specialist does not match the time slot.");
        }
        if (!Boolean.TRUE.equals(slot.getAvailable())) {
            throw new MsgException("This time slot is occupied, please choose another one.");
        }
        String normalizedIntentId = safeTrim(paymentIntentId);
        if (normalizedIntentId.isBlank()) {
            throw new MsgException("Payment intent id cannot be empty");
        }
        // Prepare Alipay transaction
        double amount = resolvePaymentAmount(request, slot);
        String currency = resolveCurrency(request, slot);
        String normalizedAmount = String.format(Locale.US, "%.2f", amount);
        String paymentToken = buildPaymentToken(normalizedIntentId);
        String subject = "Booking " + normalizedIntentId;
        String alipayQrRawCode = alipayGatewayService.precreate(paymentToken, normalizedAmount, subject);
        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=280x280&data="
                + URLEncoder.encode(alipayQrRawCode, StandardCharsets.UTF_8);

        redisTemplate.opsForValue().set(
                PAYMENT_DRAFT_KEY + normalizedIntentId,
                new PaymentDraft(paymentToken, paymentToken, amount, currency, userId, specialistId, slotId),
                PAYMENT_TTL);

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
    /**
     * Synchronizes payment status with Alipay.
     * Checks if the transaction is successful and updates the local Redis draft.
     */
    public ConfirmBookingPaymentResult confirmBookingPayment(String userId, String paymentIntentId, ConfirmBookingPaymentRequest request) {
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
        // Query Alipay gateway for actual transaction status
        String tradeStatus = alipayGatewayService.queryTradeStatus(draft.getOutTradeNo());
        if (!isAlipaySuccess(tradeStatus)) {
            throw new MsgException("支付未完成，当前状态: " + safeTrim(tradeStatus));
        }
        // Update local state to Paid
        draft.setPaid(true);
        redisTemplate.opsForValue().set(
                PAYMENT_DRAFT_KEY + paymentIntentId,
                draft,
                PAYMENT_TTL
        );
        redisTemplate.opsForZSet().remove(userUnpaidKey(userId), paymentIntentId);
        return new ConfirmBookingPaymentResult(paymentIntentId, draft.getPaymentId(), "SUCCESS", BookingStatus.Pending);
    }
    /**
     * For development/testing: bypasses Alipay to mark a draft as paid.
     */
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
    /**
     * Processes asynchronous callback notifications from Alipay.
     * Marks the corresponding PaymentDraft as paid in Redis.
     */
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
    /**
     * Lists all active (unexpired and unpaid) payment intents for a user.
     */
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
        // Sort by longest remaining time first
        items.sort((a, b) -> Long.compare(b.getRemainingSeconds() == null ? 0 : b.getRemainingSeconds(),
                a.getRemainingSeconds() == null ? 0 : a.getRemainingSeconds()));
        return new UnpaidPaymentsResult(items);
    }

    public UnpaidPaymentItemResult getUnpaidPayment(String userId, String paymentIntentId) {
        return loadUnpaidItem(userId, paymentIntentId, Instant.now().toEpochMilli(), false);
    }
    /**
     * Regenerates an Alipay QR code for an existing unpaid draft.
     * Useful if the user closed the payment page but wants to try again within the 15-min window.
     */
    public CreateBookingPaymentResult resumeUnpaidPayment(String userId, String paymentIntentId) {
        String normalizedIntentId = safeTrim(paymentIntentId);
        if (normalizedIntentId.isBlank()) {
            throw new MsgException("PaymentIntent ID cannot be empty");
        }
        PaymentDraft draft = (PaymentDraft) redisTemplate.opsForValue().get(PAYMENT_DRAFT_KEY + normalizedIntentId);
        if (draft == null) {
            redisTemplate.opsForZSet().remove(userUnpaidKey(userId), normalizedIntentId);
            throw new MsgException("The unpaid order does not exist or has expired.");
        }
        if (!userId.equals(draft.getCustomerId())) {
            throw new MsgException("No permission to operate this payment order.");
        }
        if (draft.isPaid()) {
            redisTemplate.opsForZSet().remove(userUnpaidKey(userId), normalizedIntentId);
            throw new MsgException("This payment order has been paid.");
        }
        // Re-validate slot availability before allowing resume
        Slot slot = slotRepository.findById(draft.getSlotId())
                .orElseThrow(() -> new MsgException("The appointment time slot does not exist"));
        if (!Boolean.TRUE.equals(slot.getAvailable())) {
            cleanupIntent(draft.getCustomerId(), normalizedIntentId, draft.getPaymentId());
            throw new MsgException("This time slot is occupied, please select another one.");
        }
        if (!safeTrim(slot.getSpecialistId()).equals(safeTrim(draft.getSpecialistId()))) {
            cleanupIntent(draft.getCustomerId(), normalizedIntentId, draft.getPaymentId());
            throw new MsgException("The specialist does not match the time slot.");
        }
        // Generate new Alipay out_trade_no for the same intent
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

    public void cancelUnpaidPayment(String userId, String paymentIntentId) {
        String normalizedIntentId = safeTrim(paymentIntentId);
        if (normalizedIntentId.isBlank()) {
            throw new MsgException("PaymentIntent ID must not be empty");
        }
        PaymentDraft draft = (PaymentDraft) redisTemplate.opsForValue().get(PAYMENT_DRAFT_KEY + normalizedIntentId);
        if (draft == null) {
            redisTemplate.opsForZSet().remove(userUnpaidKey(userId), normalizedIntentId);
            throw new MsgException("Unpaid order does not exist or has expired");
        }
        if (!userId.equals(draft.getCustomerId())) {
            throw new MsgException("No authority to operate this payment order.");
        }
        if (draft.isPaid()) {
            redisTemplate.opsForZSet().remove(userUnpaidKey(userId), normalizedIntentId);
            throw new MsgException("This payment order has been paid and cannot be cancelled.");
        }
        cleanupIntent(userId, normalizedIntentId, draft.getPaymentId());
    }
    /**
     * Fetches a paginated list of bookings owned by the current user, with optional status and date filters.
     */
    public BookingPageResult getMyBookings(String userId, String status, Integer page, Integer pageSize, String from, String to) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;

        List<Booking> bookings;
        BookingStatus bookingStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                bookingStatus = BookingStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new MsgException("Invalid status：" + status);
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
            throw new MsgException("Incorrect date format：" + value);
        }
    }
    /**
     * Retrieves comprehensive information for a specific booking.
     * Aggregates data from Booking, Slot, and User (Specialist/Customer) entities.
     *
     * @param bookingId The unique identifier of the booking.
     * @return A Value Object (VO) containing enriched booking details for the UI.
     */
    public SingleBookingVo getSingleBookingInfo(String bookingId){
        Booking booking = bookingRepository.getBookingById(bookingId);
        Slot slot = slotRepository.getSlotById(booking.getSlotId());
        User specialist = userRepository.findById(booking.getSpecialistId());
        String specialistName = specialist != null ? specialist.getName() : booking.getSpecialistId();
        String customerName = setNameInfo(booking.getCustomerId());
        return SingleBookingVo.fromBooking(booking, slot, specialistName ,customerName);
    }
    /**
     * Helper method to retrieve a user's name by their ID.
     *
     * @param userId The ID of the user to look up.
     * @return The user's name string.
     */
    public String setNameInfo(String userId){
        User user = userRepository.getUserById(userId);
        return user.getName();
    }
    /**
     * Validates ownership of a booking.
     * Fetches the booking and ensures it belongs to the requesting user to prevent unauthorized access.
     *
     * @param userId    The ID of the user attempting the operation.
     * @param bookingId The ID of the booking to retrieve.
     * @return The Booking entity if authorized.
     * @throws MsgException if the booking is not found or user lacks permission.
     */
    private Booking getOwnedBooking(String userId, String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new MsgException("预约不存在"));
        if (!userId.equals(booking.getCustomerId())) {
            throw new MsgException("无权限操作该预约");
        }
        return booking;
    }
    /**
     * Resolves the final payment amount for a transaction.
     * Prioritizes the amount specified in the request, falling back to the default slot price.
     *
     * @param request The payment request containing a potential custom amount.
     * @param slot    The time slot containing the default price.
     * @return The validated payment amount as a double.
     */
    private double resolvePaymentAmount(CreateBookingPaymentRequest request, Slot slot) {
        double fallback = slot.getAmount() == null ? 0.0 : slot.getAmount().doubleValue();
        if (request == null || request.getAmount() == null) {
            return fallback;
        }
        return request.getAmount() < 0 ? fallback : request.getAmount();
    }
    /**
     * Determines the currency for the transaction.
     * Priority: 1. Slot configuration -> 2. Request parameter -> 3. Default (CNY).
     *
     * @param request The payment request containing potential currency info.
     * @param slot    The time slot containing default currency info.
     * @return A trimmed currency string (e.g., "USD", "CNY").
     */
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
    /**
     * Loads a single unpaid payment item from Redis and calculates its remaining TTL (Time To Live).
     * This method performs several integrity checks:
     * 1. Existence: Removes orphaned IDs from the ZSet if the draft has expired.
     * 2. Ownership: Ensures the requesting user owns the payment draft.
     * 3. Status: Filters out already-paid drafts.
     * 4. Expiration: Cleans up the intent if the remaining time has reached zero.
     *
     * @param userId          The ID of the user requesting the item.
     * @param paymentIntentId The unique identifier for the payment session.
     * @param nowMs           The current system time in milliseconds.
     * @param silent          If true, returns null on permission failure instead of throwing an exception.
     * @return An UnpaidPaymentItemResult populated with slot details and TTL, or null if invalid.
     */
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
            throw new MsgException("No permission to view this payment order.");
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
    /**
     * Checks if the status string returned by Alipay indicates a successful transaction.
     *
     * @param status The trade status string from Alipay.
     * @return true if the trade is finished or successfully paid.
     */
    private boolean isAlipaySuccess(String status) {
        return "TRADE_SUCCESS".equalsIgnoreCase(status) || "TRADE_FINISHED".equalsIgnoreCase(status);
    }
    /**
     * Generates a unique "out_trade_no" for Alipay transactions.
     * Combines a prefix, current timestamp, and a sanitized version of the booking ID
     * to ensure uniqueness across multiple payment attempts for the same booking.
     *
     * @param bookingId The internal booking ID or payment intent ID.
     * @return A unique payment token string.
     */
    private String buildPaymentToken(String bookingId) {
        long now = System.currentTimeMillis();
        String compactId = bookingId == null ? "booking" : bookingId.replace("-", "");
        if (compactId.length() > 20) {
            compactId = compactId.substring(0, 20);
        }
        return "BK" + now + compactId;
    }
    /**
     * Cancels an existing booking and releases the associated slot back to the public pool.
     */
    @Transactional
    public BookingActionResult cancelBooking(String id) {
        // get booking details by id
        Booking booking = bookingRepository.getBookingById(id);
        // verify: only bookings in 'Confirmed' or 'Pending' are eligible for cancellation
        if (booking.getStatus() != BookingStatus.Confirmed && booking.getStatus() != BookingStatus.Pending) {
            throw new MsgException("The current appointment status cannot be cancelled.");
        }
        // update booking status to 'cancelled'
        booking.setStatus(BookingStatus.Cancelled);
        bookingRepository.save(booking);
        // get the cancelled slot and set it available
        Slot slot = slotRepository.getSlotById(booking.getSlotId());
        slot.setAvailable(true);

        return new BookingActionResult(id, BookingStatus.Cancelled);
    }
    /**
     * Reschedules an existing booking to a new time slot.
     * Releases the old slot and locks the new one in a single transaction.
     * Sends email notifications to both the customer and the specialist.
     */
    @Transactional
    public void rescheduleBooking(String bookingId, String newSlotId) {
        // get booking details
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new MsgException("The appointment does not exist"));
        // check if the current booking status allows rescheduling
        if (booking.getStatus() == BookingStatus.Cancelled || booking.getStatus() == BookingStatus.Completed) {
            throw new MsgException("This appointment cannot be rescheduled.");
        }
        // validate new slot existence and availability
        Slot newSlot = slotRepository.findById(newSlotId)
                .orElseThrow(() -> new MsgException("The new time slot does not exist"));
        if (!newSlot.getAvailable()) {
            throw new MsgException("The new time slot is unavailable.");
        }
        // ensure the new slot belongs to the same specialist
        if (!newSlot.getSpecialistId().equals(booking.getSpecialistId())) {
            throw new MsgException("The new time slot does not match the original specialist.");
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

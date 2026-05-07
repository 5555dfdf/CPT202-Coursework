package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.dto.request.CreateSpecialistRequest;
import org.example.coursework3.dto.request.EditSpecialistRequest;
import org.example.coursework3.dto.request.SlotRequest;
import org.example.coursework3.dto.response.BookingPageResult;
import org.example.coursework3.entity.*;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.*;
import org.example.coursework3.vo.AdminSlotVo;
import org.example.coursework3.vo.BatchUpdateSpecialistStatusResultVo;
import org.example.coursework3.vo.SlotVo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final SpecialistsRepository specialistsRepository;
    private final ExpertiseRepository expertiseRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     *
     * @param request (name, userEmail, password, expertiseIds[], bios)
     *                operation (add/delete/query/update)
     *                to other components of the system(Redis, Mysql)
     * @return Specialist specialist
     */
    @Transactional
    public Specialist createSpecialist(CreateSpecialistRequest request) {
        if (userRepository.findByEmail(request.getUserEmail()).isPresent()) {
            throw new MsgException("This email is already registered.");
        }

        // 1. save users
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getUserEmail());
        user.setRole(Role.Specialist);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        // 2. create specialists
        Specialist specialist = new Specialist(user.getId(), user.getName(), request.getPrice(), request.getBio());
        List<Expertise> expertiseList = new ArrayList<>();
        for (String expertiseId : request.getExpertiseIds()) {
            Expertise expertise = expertiseRepository.findById(expertiseId)
                    .orElseThrow(() -> new MsgException("Expertise does not exist."));
            expertiseList.add(expertise);
        }
        specialist.setExpertises(expertiseList);
        specialistsRepository.save(specialist);
        return specialist;
    }
    /**
     * Updates an existing specialist's information.
     * Synchronizes name changes between the User and Specialist tables.
     */
    public Specialist updateSpecialist(String id, EditSpecialistRequest request) {
        Specialist specialist;
        try {
            specialist = specialistsRepository.getByUserId(id);
        } catch (Exception e) {
            throw new MsgException("This specialist does not exist");
        }
        // Update user-level details
        if (request.getName() != null) {
            User user = userRepository.findById(id);
            user.setName(request.getName());
            userRepository.save(user);
            specialist.setName(request.getName());
        }
        if (request.getBio() != null) {
            specialist.setBio(request.getBio());
        }
        if (request.getPrice() != null) {
            specialist.setPrice(request.getPrice());
        }
        if (request.getStatus() != null) {
            specialist.setStatus(request.getStatus());
        }
        // Refresh expertise associations
        if (request.getExpertiseIds() != null) {
            List<Expertise> expertiseList = new ArrayList<>();
            for (String expertiseId : request.getExpertiseIds()) {
                Expertise expertise = expertiseRepository.findById(expertiseId)
                        .orElseThrow(() -> new MsgException("Expertise does not exist."));
                expertiseList.add(expertise);
            }
            specialist.setExpertises(expertiseList);
        }
        specialistsRepository.save(specialist);
        return specialist;
    }
    /**
     * Updates the active status of a specialist and invalidates their current session.
     */
    @Transactional
    public Specialist updateSpecialistStatus(String id, SpecialistStatus status) {
        Specialist specialist;
        try {
            specialist = specialistsRepository.getByUserId(id);
        } catch (Exception e) {
            throw new MsgException("This specialist does not exist");
        }
        specialist.setStatus(status);
        deleteTokenByUserId(id);
        specialistsRepository.save(specialist);
        return specialist;
    }

    /**
     * Clears authentication tokens from Redis for a specific user.
     * Handles the bidirectional mapping: auth:user:{uid} -> token AND auth:token:{token} -> uid.
     */
    public void deleteTokenByUserId(String userId) {

        String userKey = "auth:user:" + userId;

        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        if (valueOps == null) {
            return;
        }

        // 1. Retrieve the active token associated with this user
        String token = redisTemplate.opsForValue().get(userKey);

        if (token != null) {
            String tokenKey = "auth:token:" + token;

            // 2. delete userId -> token
            redisTemplate.delete(tokenKey);
        }

        // 3. delete userId -> token
        redisTemplate.delete(userKey);
    }
    /**
     * Performs a batch update on multiple specialists' statuses.
     * Aggregates successes and failures into a result VO.
     */
    @Transactional
    public BatchUpdateSpecialistStatusResultVo batchUpdateSpecialistStatus(List<String> ids, SpecialistStatus status) {
        if (ids == null || ids.isEmpty()) {
            throw new MsgException("ids can't be null");
        }
        if (status == null) {
            throw new MsgException("status can't be null");
        }

        Set<String> seen = new HashSet<>();
        List<String> successIds = new ArrayList<>();
        List<BatchUpdateSpecialistStatusResultVo.BatchFailureVo> failures = new ArrayList<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                failures.add(new BatchUpdateSpecialistStatusResultVo.BatchFailureVo(id, "id can't be null"));
                continue;
            }
            String trimmedId = id.trim();
            if (!seen.add(trimmedId)) {
                continue;
            }
            try {
                updateSpecialistStatus(trimmedId, status);
                successIds.add(trimmedId);
            } catch (Exception ex) {
                failures.add(new BatchUpdateSpecialistStatusResultVo.BatchFailureVo(trimmedId, ex.getMessage()));
            }
        }

        return new BatchUpdateSpecialistStatusResultVo(
                successIds.size(),
                failures.size(),
                successIds,
                failures
        );
    }
    /**
     * Generates a CSV string containing all specialists sorted by creation date (descending).
     */
    @Transactional(readOnly = true)
    public String exportSpecialistsCsv() {
        List<Specialist> specialists = specialistsRepository.findAll().stream()
                .sorted((a, b) -> {
                    LocalDateTime left = a.getCreatedAt() == null ? LocalDateTime.MIN : a.getCreatedAt();
                    LocalDateTime right = b.getCreatedAt() == null ? LocalDateTime.MIN : b.getCreatedAt();
                    return right.compareTo(left);
                })
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("id,name,status,price,bio,expertiseNames,createdAt,updatedAt\n");
        for (Specialist specialist : specialists) {
            String expertiseNames = specialist.getExpertises().stream()
                    .map(Expertise::getName)
                    .collect(Collectors.joining(";"));
            csv.append(csvField(specialist.getUserId())).append(',')
                    .append(csvField(specialist.getName())).append(',')
                    .append(csvField(specialist.getStatus() == null ? null : specialist.getStatus().name())).append(',')
                    .append(csvField(specialist.getPrice() == null ? null : specialist.getPrice().toPlainString())).append(',')
                    .append(csvField(specialist.getBio())).append(',')
                    .append(csvField(expertiseNames)).append(',')
                    .append(csvField(specialist.getCreatedAt() == null ? null : specialist.getCreatedAt().toString())).append(',')
                    .append(csvField(specialist.getUpdatedAt() == null ? null : specialist.getUpdatedAt().toString()))
                    .append('\n');
        }
        return csv.toString();
    }
    /**
     * Deletes both the Specialist profile and the associated User account.
     */
    @Transactional
    public void deleteSpecialist(String id) {
        User user = userRepository.getUserById(id);
        Specialist specialist = specialistsRepository.getByUserId(id);
        specialistsRepository.delete(specialist);
        userRepository.delete(user);
    }
    /**
     * Creates a new Expertise category for specialists to subscribe to.
     */
    public Expertise createExpertise(String name, String description) {
        if (expertiseRepository.existsByName(name)) {
            throw new MsgException("This expertise has already existed!");
        }
        Expertise expertise = new Expertise();
        expertise.setName(name);
        expertise.setDescription(description);
        expertiseRepository.save(expertise);
        return expertise;
    }
    /**
     * Updates existing Expertise category details.
     */
    @Transactional
    public Expertise updateExpertise(String id, String name, String description) {
        Expertise expertise = null;
        try {
            expertise = expertiseRepository.getExpertiseById(id);
        } catch (Exception e) {
            throw new MsgException("This expertise does not exist!");
        }
        expertise.setName(name);
        expertise.setDescription(description);
        expertiseRepository.save(expertise);
        return expertise;
    }
    /**
     * Removes an Expertise category from the system.
     */
    public void deleteExpertise(String id) {
        if (!expertiseRepository.existsById(id)) {
            throw new MsgException("Please enter correct expertise ID");
        }
        expertiseRepository.deleteById(id);
    }
    /**
     * Filters and lists time slots based on specialist, date range, and availability.
     */
    public List<AdminSlotVo> listSlots(String specialistId, String date, String from, String to, Boolean available) {
        List<Slot> slots;
        if (specialistId != null && !specialistId.isBlank()) {
            slots = slotRepository.findBySpecialistId(specialistId.trim());
        } else {
            slots = slotRepository.findAll();
        }

        LocalDate filterDate = parseLocalDateOrNull(date);
        LocalTime fromTime = parseLocalTimeOrNull(from);
        LocalTime toTime = parseLocalTimeOrNull(to);

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        List<AdminSlotVo> result = new ArrayList<>();

        for (Slot s : slots) {
            if (s == null) continue;
            if (available != null && !Objects.equals(available, s.getAvailable())) continue;

            LocalDateTime start = s.getStartTime();
            LocalDateTime end = s.getEndTime();
            if (start == null || end == null) continue;

            if (filterDate != null && !filterDate.equals(start.toLocalDate())) continue;

            if (fromTime != null && start.toLocalTime().isBefore(fromTime)) continue;
            if (toTime != null && start.toLocalTime().isAfter(toTime)) continue;

            AdminSlotVo vo = new AdminSlotVo();
            vo.setId(s.getId());
            vo.setSpecialistId(s.getSpecialistId());
            vo.setDate(start.toLocalDate().toString());
            vo.setStart(start.toLocalTime().format(timeFmt));
            vo.setEnd(end.toLocalTime().format(timeFmt));
            vo.setAvailable(s.getAvailable());
            vo.setAmount(s.getAmount());
            vo.setCurrency(s.getCurrency());
            vo.setDuration(s.getDuration());
            vo.setType(s.getType());
            vo.setDetail(s.getDetail());
            result.add(vo);
        }

        return result;
    }
    /**
     * Creates a new appointment slot for a specialist.
     * Validates that the new slot does not overlap with existing slots for that specialist.
     */
    public AdminSlotVo createSlot(SlotRequest request) {
        if (request == null) {
            throw new MsgException("Payload can't be null");
        }
        String specialistId = request.getSpecialistId();
        String date = request.getDate();
        String from = request.getStart();
        String to = request.getEnd();
        Boolean available = request.getAvailable();
        if (specialistId == null || specialistId.isBlank()) {
            throw new MsgException("specialistId can't be null");
        }
        if (date == null || date.isBlank() || from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new MsgException("date/start/end can't be null");
        }

        List<Slot> slots = slotRepository.findBySpecialistId(specialistId.trim());
        LocalDateTime startNew = parseDateAndTime(date.trim(), from.trim());
        LocalDateTime endNew = parseDateAndTime(date.trim(), to.trim());
        if (!startNew.isBefore(endNew)) {
            throw new MsgException("The start time must be earlier than the end time.");
        }

        for (Slot old : slots) {
            if (old == null) continue;
            if (isTimeOverlap(startNew, endNew, old.getStartTime(), old.getEndTime())) {
                throw new MsgException("Time slot conflict!");
            }
        }

        Slot slot = new Slot();
        slot.setSpecialistId(specialistId.trim());
        slot.setStartTime(startNew);
        slot.setEndTime(endNew);
        slot.setAvailable(available);
        slot.setAmount(normalizeAmount(request.getAmount()));
        slot.setCurrency(normalizeCurrency(request.getCurrency()));
        slot.setDuration(validateDuration(request.getDuration()));
        slot.setType(normalizeType(request.getType()));
        slot.setDetail(normalizeDetail(request.getDetail()));
        slotRepository.save(slot);
        return AdminSlotVo.form(slot);
    }
    /**
     * Updates an existing time slot.
     * Prevents modification of status if there are active bookings (Pending/Confirmed).
     */
    public AdminSlotVo updateSlot(String id, SlotRequest request) {
        Slot slot = slotRepository.getSlotById(id);
        if (slot == null) {
            throw new MsgException("Invalid time slot, unable to edit.");
        }
        if (request == null) {
            throw new MsgException("Request body cannot be empty");
        }

        String date = request.getDate();
        String startStr = request.getStart();
        String endStr = request.getEnd();
        if (date == null || date.isBlank() || startStr == null || startStr.isBlank() || endStr == null || endStr.isBlank()) {
            throw new MsgException("date/start/end cannot be empty");
        }

        LocalDateTime startNew = parseDateAndTime(date.trim(), startStr.trim());
        LocalDateTime endNew = parseDateAndTime(date.trim(), endStr.trim());
        LocalDateTime now = LocalDateTime.now();
        if (startNew.isBefore(now)){
            throw new MsgException("The start time cannot be later than the current time.");
        }
        if (!startNew.isBefore(endNew)) {
            throw new MsgException("The start time must be earlier than the end time.");
        }


        List<Slot> slots = slotRepository.findBySpecialistId(slot.getSpecialistId());
        for (Slot old : slots) {
            if (old == null) continue;
            if (old.getId() != null && old.getId().equals(slot.getId())) continue;
            if (isTimeOverlap(startNew, endNew, old.getStartTime(), old.getEndTime())) {
                throw new MsgException("Time slot conflict!");
            }
        }

        slot.setStartTime(startNew);
        slot.setEndTime(endNew);
        boolean free = whetherFree(id);
        if (free&&request.getAvailable() != null) {
            slot.setAvailable(request.getAvailable());
        }else {
            throw new MsgException("This time slot has existing reservations; status cannot be modified.");
        }
        if (request.getAmount() != null) {
            slot.setAmount(normalizeAmount(request.getAmount()));
        }
        if (request.getCurrency() != null) {
            slot.setCurrency(normalizeCurrency(request.getCurrency()));
        }
        if (request.getDuration() != null) {
            slot.setDuration(validateDuration(request.getDuration()));
        }
        if (request.getType() != null) {
            slot.setType(normalizeType(request.getType()));
        }
        if (request.getDetail() != null) {
            slot.setDetail(normalizeDetail(request.getDetail()));
        }
        slotRepository.save(slot);
        return AdminSlotVo.form(slot);
    }
    /**
     * Checks if a slot is "free" (has no active Pending or Confirmed bookings).
     */
    public boolean whetherFree(String slotId){
        List<Booking> bookingList = bookingRepository.getBookingBySlotId(slotId);
        for (Booking booking : bookingList){
            if (booking.getStatus()==BookingStatus.Pending||booking.getStatus()==BookingStatus.Confirmed){
                return false;
            }
        }
        return true;
    }


    public void deleteSlot(String id) {
        if (id == null || id.isBlank()) {
            throw new MsgException("slotId cannot be empty");
        }
        if (!slotRepository.existsById(id)) {
            throw new MsgException("Time slot does not exist");
        }
        slotRepository.deleteById(id);
    }

    private LocalDate parseLocalDateOrNull(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return LocalDate.parse(v.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new MsgException("Date format error：" + v);
        }
    }

    private LocalTime parseLocalTimeOrNull(String v) {
        if (v == null || v.isBlank()) return null;
        String t = v.trim();
        try {
            return LocalTime.parse(t);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(t).toLocalTime();
        } catch (DateTimeParseException ignored) {
        }
        throw new MsgException("Date format error：" + v);
    }

    private LocalDateTime parseDateAndTime(String date, String time) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        // Parse date and time strings into LocalDate/LocalTime
        LocalDate localDate = LocalDate.parse(date, dateFormatter);
        LocalTime localTime = LocalTime.parse(time, timeFormatter);

        // Concat into LocalDateTime and return
        return LocalDateTime.of(localDate, localTime);
    }

    private boolean isTimeOverlap(LocalDateTime newStart, LocalDateTime newEnd,
                                  LocalDateTime oldStart, LocalDateTime oldEnd) {
        return newStart.isBefore(oldEnd) && oldStart.isBefore(newEnd);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new MsgException("Amount cannot be less than 0");
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String normalizeCurrency(String currency) {
        String value = currency == null ? "CNY" : currency.trim().toUpperCase();
        if (value.isEmpty()) {
            value = "CNY";
        }
        if (value.length() > 10) {
            throw new MsgException("The length of currency cannot exceed 10");
        }
        return value;
    }

    private Integer validateDuration(Integer duration) {
        if (duration == null) {
            throw new MsgException("Duration cannot be empty");
        }
        if (duration <= 0) {
            throw new MsgException("Duration must be greater than 0");
        }
        return duration;
    }

    private String normalizeType(String type) {
        String value = type == null ? "online" : type.trim().toLowerCase();
        if (value.isEmpty()) {
            value = "online";
        }
        if (value.length() > 20) {
            throw new MsgException("The length of type cannot exceed 20");
        }
        return value;
    }

    private String normalizeDetail(String detail) {
        if (detail == null) {
            return null;
        }
        String value = detail.trim();
        return value.isEmpty() ? null : value;
    }

    private String csvField(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
    /**
     * Lists all bookings with manual pagination and enrichment of user/slot names.
     */
    public BookingPageResult listBookings(Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        List<Booking> bookingList = bookingRepository.findAll();
        for (Booking booking : bookingList){
            booking.setTime(setTimeInfo(booking.getSlotId()));
            booking.setCustomerName(setNameInfo(booking.getCustomerId()));
            booking.setSpecialistName(setNameInfo(booking.getSpecialistId()));
        }
        int total = bookingList.size();
        int start = Math.min((safePage - 1) * safePageSize, total);
        int end = Math.min(start + safePageSize, total);
        List<Booking> pageItems = bookingList.subList(start, end);
        return BookingPageResult.of(pageItems, total, safePage, safePageSize);
    }
    /**
     * Formats slot time into a readable string.
     */
    public String setTimeInfo(String slotId){
        DateTimeFormatter timeFmtDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFmtTime = DateTimeFormatter.ofPattern("HH:mm");
        Slot slot = slotRepository.getSlotById(slotId);
        return slot.getStartTime().toLocalDate().format(timeFmtDate) + " " + slot.getStartTime().toLocalTime().format(timeFmtTime) + "-" +
                slot.getEndTime().toLocalTime().format(timeFmtTime);
    }

    public String setNameInfo(String userId){
        User user = userRepository.getUserById(userId);
        return user.getName();
    }

    @Transactional
    public AdminSlotVo getSingleSlotInfo(String id) {
        Slot slot = slotRepository.getById(id);
        return AdminSlotVo.form(slot);

    }
    /**
     * Exports all bookings to a CSV format for administrative reporting.
     */
    public String exportBookingssCsv() {
        List<Booking> bookings = bookingRepository.findAll().stream()
                .sorted((a, b) -> {
                    LocalDateTime left = a.getCreatedAt() == null ? LocalDateTime.MIN : a.getCreatedAt();
                    LocalDateTime right = b.getCreatedAt() == null ? LocalDateTime.MIN : b.getCreatedAt();
                    return right.compareTo(left);
                })
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("booking_id,specialist_name,customer_name,status,amount,start_time,ent_time,detail,created_at,updated_at\n");
        for (Booking booking : bookings) {
            Slot slot = slotRepository.getSlotById(booking.getSlotId());
            User customer = userRepository.findById(booking.getCustomerId());
            User specialist = userRepository.findById(booking.getSpecialistId());
            SlotVo slotVo = SlotVo.fromSlot(slot,booking, customer.getName());
            csv.append(csvField(booking.getId())).append(',')
                    .append(csvField(specialist.getName())).append(',')
                    .append(csvField(slotVo.getCustomerName())).append(',')
                    .append(csvField(booking.getStatus() == null ? null : booking.getStatus().name())).append(',')
                    .append(csvField(String.valueOf(slotVo.getAmount()))).append(" ").append(slotVo.getCurrency()).append(',')
                    .append(csvField(slotVo.getStart())).append(',')
                    .append(csvField(slotVo.getEnd())).append(',')
                    .append(csvField(slotVo.getDetail())).append(',')
                    .append(csvField(booking.getCreatedAt() == null ? null : booking.getCreatedAt().toString())).append(',')
                    .append(csvField(booking.getUpdatedAt() == null ? null : booking.getUpdatedAt().toString()))
                    .append('\n');
        }
        return csv.toString();
    }
}

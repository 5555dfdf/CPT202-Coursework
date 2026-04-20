package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.coursework3.dto.request.CreateBookingRequest;
import org.example.coursework3.dto.response.BookingActionResult;
import org.example.coursework3.dto.response.BookingPageResult;
import org.example.coursework3.dto.response.CreateBookingResult;
import org.example.coursework3.entity.*;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.BookingHistoryRepository;
import org.example.coursework3.repository.BookingRepository;
import org.example.coursework3.repository.SlotRepository;
import org.example.coursework3.repository.UserRepository;
import org.example.coursework3.vo.MyBookingVo;
import org.example.coursework3.vo.SingleBookingVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerBookingService {
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final AliyunMailService aliyunMailService;
    private final BookingHistoryRepository bookingHistoryRepository;

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

    // Cancel booking
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

//        try {
//            User specialist = userRepository.findById(booking.getSpecialistId());
//            if (specialist != null && specialist.getEmail() != null) {
//                String timeRange = slot.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " — " +
//                        slot.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
//                aliyunMailService.sendCancellationNoticeToSpecialist(specialist.getEmail(), timeRange);
//            }
//        } catch (Exception e) {
//            log.warn("发送专家取消通知失败: {}", e.getMessage());
//        }
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

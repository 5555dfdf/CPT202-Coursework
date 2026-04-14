package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.dto.request.CreateBookingRequest;
import org.example.coursework3.dto.response.BookingPageResult;
import org.example.coursework3.dto.response.CreateBookingResult;
import org.example.coursework3.entity.Booking;
import org.example.coursework3.entity.BookingStatus;
import org.example.coursework3.entity.Slot;
import org.example.coursework3.entity.User;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.BookingRepository;
import org.example.coursework3.repository.SlotRepository;
import org.example.coursework3.repository.UserRepository;
import org.example.coursework3.vo.MyBookingVo;
import org.example.coursework3.vo.SingleBookingVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerBookingService {
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final AliyunMailService aliyunMailService;

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
        return SingleBookingVo.fromBooking(booking, slot, specialistName);
    }


//    @Transactional
//    public void cancelBooking(String userId, String bookingId) {
//        Booking booking = bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new MsgException("未找到该预约记录"));
//
//        if (!booking.getCustomerId().equals(userId)) {
//            throw new MsgException("您无权取消此预约");
//        }
//
//        if (booking.getStatus() != BookingStatus.Confirmed && booking.getStatus() != BookingStatus.Pending) {
//            throw new MsgException("当前预约状态无法执行取消操作");
//        }
//
//        booking.setStatus(BookingStatus.Cancelled);
//        bookingRepository.save(booking);
//
//
//        Slot slot = slotRepository.findById(booking.getSlotId())
//                .orElseThrow(() -> new MsgException("关联的时段不存在"));
//        slot.setAvailable(true);
//        slotRepository.save(slot);
//
//        try {
//            User specialist = userRepository.findById(booking.getSpecialistId());
//            if (specialist != null && specialist.getEmail() != null) {
//                String timeRange = slot.getStartTime().toString() + " — " + slot.getEndTime().toString();
//                aliyunMailService.sendCancellationNoticeToSpecialist(specialist.getEmail(), timeRange);
//            }
//        } catch (Exception e) {
//            System.err.println("发送专家取消通知失败: " + e.getMessage());
//        }
//    }
}

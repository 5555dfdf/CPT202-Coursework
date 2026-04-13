package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.dto.request.CreateBookingRequest;
import org.example.coursework3.dto.response.CreateBookingResult;
import org.example.coursework3.entity.*;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.BookingRepository;
import org.example.coursework3.repository.SlotRepository;
import org.example.coursework3.repository.SpecialistsRepository;
import org.example.coursework3.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (!slot.getAvailable()) {
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

    @Transactional
    public void cancelBooking(String userId, String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new MsgException("未找到该预约记录"));

        if (!booking.getCustomerId().equals(userId)) {
            throw new MsgException("您无权取消此预约");
        }

        if (booking.getStatus() != BookingStatus.Confirmed && booking.getStatus() != BookingStatus.Pending) {
            throw new MsgException("当前预约状态无法执行取消操作");
        }

        booking.setStatus(BookingStatus.Cancelled);
        bookingRepository.save(booking);


        Slot slot = slotRepository.findById(booking.getSlotId())
                .orElseThrow(() -> new MsgException("关联的时段不存在"));
        slot.setAvailable(true);
        slotRepository.save(slot);

        try {
            User specialist = userRepository.findById(booking.getSpecialistId());
            if (specialist != null && specialist.getEmail() != null) {
                String timeRange = slot.getStartTime().toString() + " — " + slot.getEndTime().toString();
                aliyunMailService.sendCancellationNoticeToSpecialist(specialist.getEmail(), timeRange);
            }
        } catch (Exception e) {
            System.err.println("发送专家取消通知失败: " + e.getMessage());
        }
    }
}
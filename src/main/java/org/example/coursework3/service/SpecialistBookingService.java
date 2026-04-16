package org.example.coursework3.service;

import lombok.extern.slf4j.Slf4j;
import org.example.coursework3.dto.response.BookingActionResult;
import org.example.coursework3.dto.response.BookingPageResult;
import org.example.coursework3.entity.*;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.*;
import org.example.coursework3.vo.BookingRequestVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SpecialistBookingService {
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private BookingHistoryRepository bookingHistoryRepository;
    @Autowired
    private AliyunMailService aliyunMailService;
    @Autowired
    private SpecialistsRepository specialistsRepository;

    public BookingPageResult getSpecialistBookings(String authHeader, String status, Integer page, Integer pageSize) {
        String token = authHeader.replace("Bearer ","");
        String specialistId = authService.getUserIdByToken(token);
        User specialist = userRepository.findById(specialistId);
        if (specialist.getRole() != Role.Specialist){
            throw new MsgException("您不是专家，无权访问");
        }
        //强转类型 从String -> BookingStatus
        Page<Booking> bookingPage;
        List<BookingRequestVo> voList = null;
        try {
            BookingStatus status1 = null;
            if (status != null && !status.isEmpty()) {
                try {
                    status1 = BookingStatus.valueOf(status);
                } catch (IllegalArgumentException e) {
                    throw new MsgException("无效的状态值：" + status);
                }
            }
            PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            try {
                if (status1 == null) {
                    bookingPage = bookingRepository.findBySpecialistId(specialistId, pageRequest);
                    System.out.println(bookingPage);
                } else {
                    bookingPage = bookingRepository.findBySpecialistIdAndStatus(specialistId, status1, pageRequest);
                    System.out.println(bookingPage);
                }
            } catch (Exception e) {
                throw new MsgException("没搜到数据");
            }

            voList = bookingPage.getContent().stream()
                    .map(booking ->{
                        User customer = userRepository.findById(booking.getCustomerId());
                        String customerName = customer.getName();
                        Slot slot = slotRepository.findById(booking.getSlotId()).orElse(null);
                        return BookingRequestVo.fromBooking(booking,customerName, slot);
                    }).toList();
        } catch (MsgException e) {
            throw new MsgException("SQL出错");
        }

        return BookingPageResult.of(voList, bookingPage.getTotalElements(),page,pageSize);
    }


    public BookingActionResult confirmBooking(String authHeader, String bookingId) {
        String token = authHeader.replace("Bearer ","");
        String specialistId = authService.getUserIdByToken(token);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(()-> new MsgException("No such reservation"));
        Slot slot = slotRepository.findById(booking.getSlotId()).orElseThrow(() -> new MsgException("No such slot"));
        if (!booking.getSpecialistId().equals(specialistId)) {
            throw new MsgException("Without right to handle this reservation");
        }
        if (booking.getStatus() != BookingStatus.Pending){
            throw new MsgException("Can only handle pending reservations");
        }
        booking.setStatus(BookingStatus.Confirmed);
        bookingRepository.save(booking);
        //发送邮件逻辑
        try {
            User customer = userRepository.findById(booking.getCustomerId());
            User specialist = userRepository.getUserById(booking.getSpecialistId());
            if (customer != null && customer.getEmail() != null) {
                aliyunMailService.sendBookingStatusNotification(specialist.getName(), customer.getEmail(), "Confirmed", null);
            }
        } catch (Exception e) {
            log.warn("Failed to send confirmation email notification: {}", e.getMessage());
        }
        slot.setAvailable(Boolean.FALSE);
        slotRepository.save(slot);
        return new BookingActionResult(bookingId, BookingStatus.Confirmed);
    }

    @Transactional
    public BookingActionResult rejectBooking(String authHeader, String bookingId, String reason) {
        String token = authHeader.replace("Bearer ","");
        String specialistId = authService.getUserIdByToken(token);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(()-> new MsgException("No such reservation"));

        if (!booking.getSpecialistId().equals(specialistId)) {
            throw new MsgException("Without right to handle this reservation");
        }
        if (booking.getStatus() != BookingStatus.Pending){
            throw new MsgException("Can only handle pending reservations");
        }
        booking.setStatus(BookingStatus.Rejected);
        booking.setNote(reason);
        bookingRepository.save(booking);
        Slot slot = slotRepository.getById(booking.getSlotId());
        slot.setAvailable(true);
        slotRepository.save(slot);
        //发送邮件
        try {
            User customer = userRepository.findById(booking.getCustomerId());
            User specialist = userRepository.getUserById(booking.getSpecialistId());
            if (customer != null && customer.getEmail() != null) {
                aliyunMailService.sendBookingStatusNotification(specialist.getName(), customer.getEmail(), "Rejected", reason);
            }
        } catch (Exception e) {
            log.warn("Failed to send rejection email notification: {}", e.getMessage());
        }
        return new BookingActionResult(bookingId, BookingStatus.Rejected);
    }

    public BookingActionResult completeBooking(String authHeader, String bookingId) {
        String token = authHeader.replace("Bearer ","");
        String specialistId = authService.getUserIdByToken(token);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(()-> new MsgException("No such reservation"));

        if (!booking.getSpecialistId().equals(specialistId)) {
            throw new MsgException("Without right to handle this reservation");
        }
        if (booking.getStatus() != BookingStatus.Confirmed){
            throw new MsgException("Can only handle Confirmed reservations");
        }
        booking.setStatus(BookingStatus.Completed);
        bookingRepository.save(booking);
        return new BookingActionResult(bookingId, BookingStatus.Completed);
    }

    @Transactional
    public void createBookingHistory(Booking booking) {
        // 1. 检查这条记录是否已经存在
        boolean exists = bookingHistoryRepository
                .existsByBookingIdAndStatus(
                        booking.getId(),
                        booking.getStatus()
                );

        if (exists) {
            BookingHistory history =
                    bookingHistoryRepository
                            .getByBookingIdAndStatus(
                                    booking.getId(),
                                    booking.getStatus()
                            );
            history.setChangedAt(LocalDateTime.now());
            log.info("该状态记录已存在，更新操作时间：{}", booking.getId());
            return;
        }

        // 2. 只创建一条历史记录
        BookingHistory history = new BookingHistory();
        history.setBookingId(booking.getId());
        history.setStatus(booking.getStatus());
        history.setReason(booking.getNote());
        history.setChangedAt(booking.getUpdatedAt());

        // 3. 只保存这一条
        bookingHistoryRepository.save(history);
    }
}




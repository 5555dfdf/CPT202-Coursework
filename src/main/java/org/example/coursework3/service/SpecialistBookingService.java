package org.example.coursework3.service;

import lombok.extern.slf4j.Slf4j;
import org.example.coursework3.dto.response.BookingActionResult;
import org.example.coursework3.dto.response.BookingPageResult;
import org.example.coursework3.entity.*;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.*;
import org.example.coursework3.vo.BookingRequestVo;
import org.example.coursework3.vo.SingleBookingVo;
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
        // extract and verify specialist identity
        String token = authHeader.replace("Bearer ","");
        String specialistId = authService.getUserIdByToken(token);
        User specialist = userRepository.findById(specialistId);
        if (specialist.getRole() != Role.Specialist){
            throw new MsgException("您不是专家，无权访问");
        }
        // parse status string to Enum
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
            // create page request sorted by creation time descending
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
            // transform Entity list to VO list with additional user/slot info
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

    // confirm booking - transfer 'Pending' to 'Confirmed'
    public BookingActionResult confirmBooking(String authHeader, String bookingId) {
        String token = authHeader.replace("Bearer ","");
        String specialistId = authService.getUserIdByToken(token);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(()-> new MsgException("No such reservation"));
        Slot slot = slotRepository.findById(booking.getSlotId()).orElseThrow(() -> new MsgException("No such slot"));
        // verify if u are a specialist and if the booking status is pending
        if (!booking.getSpecialistId().equals(specialistId)) {
            throw new MsgException("Without right to handle this reservation");
        }
        if (booking.getStatus() != BookingStatus.Pending){
            throw new MsgException("Can only handle pending reservations");
        }
        // update status
        booking.setStatus(BookingStatus.Confirmed);
        bookingRepository.save(booking);
        //发送邮件逻辑
//        try {
//            User customer = userRepository.findById(booking.getCustomerId());
//            Specialist specialist = specialistsRepository.getByUserId(booking.getSpecialistId());
//            if (customer != null && customer.getEmail() != null) {
//                aliyunMailService.sendBookingStatusNotification(specialist.getName(), customer.getEmail(), "Confirmed", null);
//            }
//        } catch (Exception e) {
//            log.warn("Failed to send confirmation email notification: {}", e.getMessage());
//        }
        // lock the slot
        slot.setAvailable(Boolean.FALSE);
        slotRepository.save(slot);
        return new BookingActionResult(bookingId, BookingStatus.Confirmed);
    }
    // reject booking - transfer 'Pending' to 'Rejected'
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
        // update status, set note
        booking.setStatus(BookingStatus.Rejected);
        booking.setNote(reason);
        bookingRepository.save(booking);

        // release the slot
        Slot slot = slotRepository.getById(booking.getSlotId());
        slot.setAvailable(true);
        slotRepository.save(slot);
//        //发送邮件
//        try {
//            User customer = userRepository.findById(booking.getCustomerId());
//            Specialist specialist = specialistsRepository.getByUserId(booking.getSpecialistId());
//            if (customer != null && customer.getEmail() != null) {
//                aliyunMailService.sendBookingStatusNotification(specialist.getName(), customer.getEmail(), "Rejected", reason);
//            }
//        } catch (Exception e) {
//            log.warn("Failed to send rejection email notification: {}", e.getMessage());
//        }
        return new BookingActionResult(bookingId, BookingStatus.Rejected);
    }
    // complete booking - transfer 'Confirmed' into 'Completed'
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
    // get detailed information for a specific booking.
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

    /**
     Records a status change in the booking history and sends email notifications.
     This method is triggered whenever a booking status is updated.
     **/
    @Transactional
    public void createBookingHistory(Booking booking) throws Exception {
        // check if the record has exists
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

        // create new history entry
        BookingHistory history = new BookingHistory();
        history.setBookingId(booking.getId());
        history.setStatus(booking.getStatus());
        history.setReason(booking.getNote());
        history.setChangedAt(booking.getUpdatedAt());

        // only save one record
        bookingHistoryRepository.save(history);


        // Notification Logic: Dispatch emails via Aliyun service
        try {
            User customer = userRepository.findById(booking.getCustomerId());
            User specialist = userRepository.findById(booking.getSpecialistId());
            Slot slot = slotRepository.getSlotById(booking.getSlotId());

            // change time range format
            String range = "";
            if (slot != null){
                range = slot.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " - " +
                        slot.getEndTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            // send to Customer
            if (customer != null && customer.getEmail()!= null){
                aliyunMailService.sendGenericStatusNotification(customer.getEmail(), "Customer", booking.getStatus().name(),range, booking.getNote());

            }
            // send to Specialist
            if (specialist != null && specialist.getEmail()!= null){
                if (booking.getStatus() == BookingStatus.Cancelled){
                    aliyunMailService.sendCancellationNoticeToSpecialist(specialist.getEmail(), range);
                }
                else aliyunMailService.sendGenericStatusNotification(specialist.getEmail(), "Specialist", booking.getStatus().name(), range, booking.getNote());

            }
        } catch (Exception e) {
            log.error("邮件通知发送失败: {}", e.getMessage());
        }
    }

    }









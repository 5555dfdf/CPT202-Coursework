package org.example.coursework3.vo;
import lombok.Data;
import org.example.coursework3.entity.Booking;
import org.example.coursework3.entity.BookingStatus;
import org.example.coursework3.entity.Slot;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class SingleBookingVo {
    private String id;
    private String customerId;
    private String specialistId;
    private String specialistName;
    private String customerName;
    private String time;
    private BookingStatus status;
    private String note;
    private String duration;
    private BigDecimal amount;
    private String currency;
    private String type;
    private String detail;
    private String createdAt;
    private String updatedAt;
    private String price;


    public static SingleBookingVo fromBooking(Booking booking, Slot slot, String specialistName , String customerName) {
        SingleBookingVo vo = new SingleBookingVo();
        vo.setId(booking.getId());
        vo.setCustomerId(booking.getCustomerId());
        vo.setSpecialistId(booking.getSpecialistId());
        vo.setSpecialistName(specialistName);
        vo.setTime(slot.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        vo.setCreatedAt(booking.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        vo.setUpdatedAt(booking.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        vo.setStatus(booking.getStatus());
        vo.setNote(booking.getNote());
        Duration duration = Duration.between(slot.getStartTime(), slot.getEndTime());
        long time = duration.toMinutes();
        vo.setDuration(time + " minutes");
        vo.setAmount(slot.getAmount());
        vo.setCurrency(slot.getCurrency());
        vo.setPrice(slot.getAmount().toString() + " " + slot.getCurrency());
        vo.setType(slot.getType());
        vo.setDetail(slot.getDetail());
        vo.setCustomerName(customerName);
        return vo;
    }
}

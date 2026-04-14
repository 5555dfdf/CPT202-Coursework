package org.example.coursework3.vo;
import lombok.Data;
import org.example.coursework3.entity.Booking;
import org.example.coursework3.entity.BookingStatus;
import org.example.coursework3.entity.Slot;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class SingleBookingVo {
    private String id;
    private String customerId;
    private String specialistId;
    private String specialistName;
    private String time;
    private BookingStatus status;
    private String note;
    private String duration;


    public static SingleBookingVo fromBooking(Booking booking, Slot slot, String specialistName) {
        SingleBookingVo vo = new SingleBookingVo();
        vo.setId(booking.getId());
        vo.setCustomerId(booking.getCustomerId());
        vo.setSpecialistId(booking.getSpecialistId());
        vo.setSpecialistName(specialistName);
        vo.setTime(slot.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        vo.setStatus(booking.getStatus());
        vo.setNote(booking.getNote());
        Duration duration = Duration.between(slot.getStartTime(), slot.getEndTime());
        long time = duration.toMinutes();
        vo.setDuration(time + " minutes");
        return vo;
    }
}

package org.example.coursework3.vo;

import lombok.Data;
import org.example.coursework3.entity.Booking;
import org.example.coursework3.entity.Slot;

import java.time.format.DateTimeFormatter;

@Data
public class MyBookingVo {
    private String id;
    private String specialistId;
    private String time;
    private String status;

    public static MyBookingVo fromBooking(Booking booking, Slot slot) {
        MyBookingVo vo = new MyBookingVo();
        vo.setId(booking.getId());
        vo.setSpecialistId(booking.getSpecialistId());
        vo.setTime(slot.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        vo.setStatus(booking.getStatus().name());
        return vo;
    }
}

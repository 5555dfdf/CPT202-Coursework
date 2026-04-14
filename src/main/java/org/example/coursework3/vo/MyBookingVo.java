package org.example.coursework3.vo;

import lombok.Data;
import org.example.coursework3.entity.Booking;
import org.example.coursework3.entity.Slot;

import java.time.format.DateTimeFormatter;

@Data
public class MyBookingVo {
    private String id;
    private String specialistId;
    private String specialistName;
    private String time;
    private String status;


    public static MyBookingVo fromBooking(Booking booking, Slot slot, String specialistName) {
        MyBookingVo vo = new MyBookingVo();
        vo.setId(booking.getId());
        vo.setSpecialistId(booking.getSpecialistId());
        vo.setSpecialistName(specialistName);
        vo.setTime(slot.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        vo.setStatus(booking.getStatus().name());
        return vo;
    }
}

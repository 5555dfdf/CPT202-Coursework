package org.example.coursework3.vo;

import lombok.Data;
import org.example.coursework3.entity.Booking;
import org.example.coursework3.entity.Slot;

import java.time.format.DateTimeFormatter;

@Data
public class BookingRequestVo {
    private String id;
    private String customerName;
    private String time;
    private String status;

    public static BookingRequestVo fromBooking(Booking booking, String customerName,Slot slot){
        DateTimeFormatter timeFmtDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFmtTime = DateTimeFormatter.ofPattern("HH:mm");
        BookingRequestVo vo = new BookingRequestVo();
        vo.setId(booking.getId());
        vo.setCustomerName(customerName);
        vo.setTime(slot.getStartTime().toLocalDate().format(timeFmtDate) + " - " + slot.getStartTime().toLocalTime().format(timeFmtTime));
        vo.setStatus(booking.getStatus().name());
        return vo;
    }
}

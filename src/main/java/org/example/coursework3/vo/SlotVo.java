package org.example.coursework3.vo;

import lombok.Data;
import org.example.coursework3.entity.Booking;
import org.example.coursework3.entity.Slot;

@Data
public class SlotVo {
    private String slotId;
    private String start;
    private String end;
    private Boolean available;
    private String bookingId;
    private String status;
    private String customerName;

    public static SlotVo fromSlot(Slot slot, Booking booking, String customerName) {
        SlotVo vo = new SlotVo();
        vo.setSlotId(slot.getId());
        vo.setStart(slot.getStartTime().toString());
        vo.setEnd(slot.getEndTime().toString());
        vo.setAvailable(slot.getAvailable());
        if (booking != null) {
            vo.setBookingId(booking.getId());
            vo.setStatus(booking.getStatus().name());
            vo.setCustomerName(customerName);
        }
        return vo;
    }
}

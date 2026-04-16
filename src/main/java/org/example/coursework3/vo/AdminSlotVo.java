package org.example.coursework3.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.coursework3.entity.Slot;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSlotVo {
    private String id;
    private String specialistId;
    private String date;
    private String start;
    private String end;
    private Boolean available;
    private BigDecimal amount;
    private String currency;
    private Integer duration;
    private String type;
    private String detail;

    public static AdminSlotVo form(Slot slot){
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        AdminSlotVo vo = new AdminSlotVo();
        vo.setId(slot.getId());
        vo.setSpecialistId(slot.getSpecialistId());
        vo.setDate(slot.getStartTime().toLocalDate().toString());
        vo.setStart(slot.getStartTime().toLocalTime().format(timeFmt));
        vo.setEnd(slot.getEndTime().toLocalTime().format(timeFmt));
        vo.setAvailable(slot.getAvailable());
        vo.setAmount(slot.getAmount());
        vo.setCurrency(slot.getCurrency());
        vo.setDuration(slot.getDuration());
        vo.setType(slot.getType());
        vo.setDetail(slot.getDetail());
        return vo;
    }
}

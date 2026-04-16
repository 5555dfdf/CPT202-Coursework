package org.example.coursework3.dto.request;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SlotRequest {
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
}
package org.example.coursework3.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PricingRuleRequest {
    private String specialistId;
    private Integer duration;
    private String type;
    private BigDecimal amount;
    private String currency;
    private String detail;
}

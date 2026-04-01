package org.example.coursework3.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PricingQuoteRequest {
    private String specialistId;
    private Integer duration;
    private String type;
}

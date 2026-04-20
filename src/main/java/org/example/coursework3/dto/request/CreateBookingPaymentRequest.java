package org.example.coursework3.dto.request;

import lombok.Data;

@Data
public class CreateBookingPaymentRequest {
    private String specialistId;
    private String slotId;
    private Double amount;
    private String currency;
    private String scene;
}

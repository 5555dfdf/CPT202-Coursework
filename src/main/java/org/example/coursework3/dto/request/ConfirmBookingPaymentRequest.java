package org.example.coursework3.dto.request;

import lombok.Data;

@Data
public class ConfirmBookingPaymentRequest {
    private String paymentId;
    private String status;
}

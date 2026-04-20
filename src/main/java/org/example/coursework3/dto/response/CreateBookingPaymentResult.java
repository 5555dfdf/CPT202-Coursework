package org.example.coursework3.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateBookingPaymentResult {
    private String paymentId;
    private String paymentToken;
    private String qrCodeUrl;
    private Double amount;
    private String currency;
}

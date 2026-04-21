package org.example.coursework3.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnpaidPaymentItemResult {
    private String paymentIntentId;
    private String paymentId;
    private String specialistId;
    private String slotId;
    private String slotLabel;
    private Double amount;
    private String currency;
    private String status;
    private String expiresAt;
    private Long remainingSeconds;
}

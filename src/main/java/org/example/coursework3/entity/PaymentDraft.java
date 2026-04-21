package org.example.coursework3.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentDraft {
    private String outTradeNo;
    private String paymentId;
    private Double amount;
    private String currency;
    private String customerId;
    private String specialistId;
    private String slotId;
    private boolean paid;

    public PaymentDraft(String outTradeNo, String paymentId, Double amount, String currency, String customerId,
                        String specialistId, String slotId) {
        this.outTradeNo = outTradeNo;
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.customerId = customerId;
        this.specialistId = specialistId;
        this.slotId = slotId;
        this.paid = false;
    }
}

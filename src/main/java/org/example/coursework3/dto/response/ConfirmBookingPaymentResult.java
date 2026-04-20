package org.example.coursework3.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.coursework3.entity.BookingStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmBookingPaymentResult {
    private String id;
    private String paymentId;
    private String paymentStatus;
    private BookingStatus status;
}

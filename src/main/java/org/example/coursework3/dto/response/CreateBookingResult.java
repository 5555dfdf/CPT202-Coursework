package org.example.coursework3.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.coursework3.entity.BookingStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateBookingResult {
    private String id;
    private String specialistId;
    private String slotId;
    private BookingStatus status = BookingStatus.Pending;


}

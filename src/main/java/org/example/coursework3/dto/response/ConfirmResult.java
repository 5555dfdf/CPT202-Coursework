package org.example.coursework3.dto.response;

import lombok.Data;
import org.example.coursework3.entity.BookingStatus;

@Data
public class ConfirmResult {
    private String id;
    private BookingStatus status = BookingStatus.Confirmed;
}

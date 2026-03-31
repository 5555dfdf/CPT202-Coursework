package org.example.coursework3.result;

import lombok.Data;
import org.example.coursework3.entity.BookingStatus;

@Data
public class ConfirmResult {
    private String id;
    private BookingStatus status = BookingStatus.Confirmed;
}

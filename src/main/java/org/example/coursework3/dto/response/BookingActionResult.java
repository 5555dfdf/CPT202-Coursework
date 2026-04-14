package org.example.coursework3.dto.response;

import lombok.Data;
import org.example.coursework3.entity.BookingStatus;

@Data
public class BookingActionResult {
    private String id;
    private BookingStatus status;

    public BookingActionResult(String id, BookingStatus status) {
        this.id = id;
        this.status = status;
    }
}

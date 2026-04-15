package org.example.coursework3.dto.request;
import lombok.Data;

@Data
public class SlotRequest {
    private String specialistId;
    private String date;
    private String start;
    private String end;
    private Boolean available = true;
}
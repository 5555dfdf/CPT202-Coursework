package org.example.coursework3.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBookingRequest {
    @NotBlank(message = "专家ID不能为空")
    private String specialistId;
    @NotBlank(message = "时段ID不能为空")
    private String slotId;
    private String note;
}

package org.example.coursework3.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExpertiseRequest {
    @NotBlank(message = "专长名称不能为空")
    private String name;
    private String description;
}

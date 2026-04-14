package org.example.coursework3.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.example.coursework3.entity.SpecialistStatus;

import java.math.BigDecimal;

@Data
public class EditSpecialistRequest {
    private String name;
    private String[] expertiseIds;
    private BigDecimal price = BigDecimal.valueOf(50);
    private String bio;
    private SpecialistStatus status;
}

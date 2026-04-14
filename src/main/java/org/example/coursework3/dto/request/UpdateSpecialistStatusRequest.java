package org.example.coursework3.dto.request;

import lombok.Data;
import org.example.coursework3.entity.SpecialistStatus;

@Data
public class UpdateSpecialistStatusRequest {
    private SpecialistStatus status;
}


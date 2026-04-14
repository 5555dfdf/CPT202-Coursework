package org.example.coursework3.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateSpecialistRequest {

    @NotEmpty(message = "姓名不能为空")
    private String name;
    @NotEmpty(message = "专长不能为空")
    @NotEmpty(message = "邮箱不能为空")
    private String userEmail;
    private String password = "123456";
    private String[] expertiseIds;
    private BigDecimal price = BigDecimal.valueOf(50);
    private String bio;
}

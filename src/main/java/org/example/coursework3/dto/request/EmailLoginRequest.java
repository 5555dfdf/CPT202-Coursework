package org.example.coursework3.dto.request;

import lombok.Data;

@Data
public class EmailLoginRequest {
    private String email;
    private String verificationCode;
}

package org.example.coursework3.dto.request;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String currentPassword;
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}

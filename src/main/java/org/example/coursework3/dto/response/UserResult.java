package org.example.coursework3.dto.response;

import lombok.Data;
import org.example.coursework3.entity.User;

@Data
public class UserResult {
    private String id;
    private String name;
    private String email;
    private String role;
    private String avatar;

    public UserResult toDTO(User user) {
        UserResult dto = new UserResult();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setAvatar(user.getAvatar());
        return dto;
    }
}

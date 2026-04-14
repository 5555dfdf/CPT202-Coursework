package org.example.coursework3.dto.response;
import lombok.Data;
import org.example.coursework3.entity.User;
import java.util.UUID;

@Data
public class RegisterResult implements AuthResult {
    private User user;
    private String token  = UUID.randomUUID().toString();

}

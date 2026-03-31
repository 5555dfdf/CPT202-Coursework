package org.example.courework3.result;
import lombok.Data;
import org.example.courework3.entity.User;
import java.util.UUID;

@Data
public class RegisterResult implements AuthResult {
    private User user;
    private String token  = UUID.randomUUID().toString();

}

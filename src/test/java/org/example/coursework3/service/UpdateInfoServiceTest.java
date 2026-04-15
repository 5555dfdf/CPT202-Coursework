package org.example.coursework3.service;

import org.example.coursework3.dto.request.UpdateSelfInfoRequest;
import org.example.coursework3.entity.Role;
import org.example.coursework3.entity.User;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UpdateInfoServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateInfoService updateInfoService;

    @Test
    void updateSelfInfo_updatesNameAndAvatar_forU1Alice() {
        User alice = userAlice();
        when(userRepository.findById("u1")).thenReturn(alice);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateSelfInfoRequest req = new UpdateSelfInfoRequest();
        req.setName("Alice");
        req.setAvatar("https://example.com/avatars/u1.png");

        User out = updateInfoService.updateSelfInfo("u1", req);

        assertEquals("Alice", out.getName());
        assertEquals("https://example.com/avatars/u1.png", out.getAvatar());
        verify(userRepository).save(alice);
    }

    @Test
    void updateSelfInfo_throwsWhenUserNotFound() {
        when(userRepository.findById("missing")).thenThrow(new RuntimeException("no row"));

        UpdateSelfInfoRequest req = new UpdateSelfInfoRequest();
        req.setName("X");

        MsgException ex = assertThrows(MsgException.class, () -> updateInfoService.updateSelfInfo("missing", req));
        assertEquals("用户不存在", ex.getMessage());
    }

    private static User userAlice() {
        User user = new User();
        user.setId("u1");
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setRole(Role.Customer);
        user.setAvatar(null);
        user.setPasswordHash("hashed_pw_1");
        return user;
    }
}

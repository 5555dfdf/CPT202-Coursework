package org.example.coursework3.service;

import org.example.coursework3.dto.response.LoginResult;
import org.example.coursework3.entity.Role;
import org.example.coursework3.entity.User;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_succeedsForAliceFromSql() {
        User alice = userAlice();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));

        User out = authService.login("alice@example.com", "hashed_pw_1");

        assertEquals("u1", out.getId());
        assertEquals("Alice", out.getName());
    }

    @Test
    void login_throwsWhenPasswordWrong() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(userAlice()));

        MsgException ex = assertThrows(MsgException.class, () -> authService.login("alice@example.com", "wrong"));
        assertEquals("密码不正确", ex.getMessage());
    }

    @Test
    void login_throwsWhenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        MsgException ex = assertThrows(MsgException.class, () -> authService.login("unknown@example.com", "x"));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void loginByCode_succeedsWhenCaptchaMatchesAlice() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:alice@example.com")).thenReturn("123456");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(userAlice()));

        User out = authService.loginByCode("alice@example.com", "123456");

        assertEquals("u1", out.getId());
    }

    @Test
    void loginByCode_throwsWhenCaptchaWrong() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:alice@example.com")).thenReturn("999999");

        MsgException ex = assertThrows(MsgException.class, () -> authService.loginByCode("alice@example.com", "123456"));
        assertEquals("验证码错误或已过期", ex.getMessage());
    }

    @Test
    void loginByCode_throwsWhenEmailNotRegistered() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:charlie@example.com")).thenReturn("123456");
        when(userRepository.findByEmail("charlie@example.com")).thenReturn(Optional.empty());

        MsgException ex = assertThrows(MsgException.class, () -> authService.loginByCode("charlie@example.com", "123456"));
        assertEquals("该邮箱未注册账号", ex.getMessage());
    }

    @Test
    void getSelfInfo_returnsAliceForU1() {
        when(userRepository.findById("u1")).thenReturn(userAlice());

        User out = authService.getSelfInfo("u1");

        assertEquals("alice@example.com", out.getEmail());
    }

    @Test
    void getUserIdByToken_returnsUserIdWhenPresent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("auth:token:tok-1")).thenReturn("u1");

        assertEquals("u1", authService.getUserIdByToken("tok-1"));
    }

    @Test
    void getUserIdByToken_throwsWhenMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("auth:token:bad")).thenReturn(null);

        MsgException ex = assertThrows(MsgException.class, () -> authService.getUserIdByToken("bad"));
        assertEquals("token无效或已过期", ex.getMessage());
    }

    @Test
    void register_succeedsForNewEmailAfterCaptcha() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:carol@example.com")).thenReturn("654321");
        when(userRepository.findByEmail("carol@example.com")).thenReturn(Optional.empty());
        User saved = new User();
        saved.setId("new-id");
        saved.setName("Carol");
        saved.setEmail("carol@example.com");
        saved.setRole(Role.Customer);
        saved.setPasswordHash("pw");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User out = authService.register("Carol", "carol@example.com", "654321", "pw");

        assertEquals("carol@example.com", out.getEmail());
        verify(redisTemplate).delete("captcha:carol@example.com");
    }

    @Test
    void register_throwsWrappedWhenBobAlreadyExistsInSql() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:bob@example.com")).thenReturn("123456");
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(userBob()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register("Bob", "bob@example.com", "123456", "pw"));
        assertNotNull(ex.getCause());
        assertEquals("该邮箱已被注册", ex.getCause().getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throwsWrappedWhenCaptchaInvalid() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:alice@example.com")).thenReturn("000000");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register("Alice", "alice@example.com", "123456", "pw"));
        assertEquals("验证码错误或已过期", ex.getCause().getMessage());
    }

    @Test
    void storeToken_writesRedisKeys() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("auth:user:u1")).thenReturn(null);

        authService.storeToken("new-token", "u1");

        verify(valueOps).set(eq("auth:token:new-token"), eq("u1"), eq(1L), eq(TimeUnit.DAYS));
        verify(valueOps).set(eq("auth:user:u1"), eq("new-token"), eq(1L), eq(TimeUnit.DAYS));
    }

    @Test
    void deleteToken_removesKeysWhenTokenMapsToUser() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("auth:token:tok-del")).thenReturn("u1");

        authService.deleteToken("tok-del");

        verify(redisTemplate).delete("auth:token:tok-del");
        verify(redisTemplate).delete("auth:user:u1");
    }

    @Test
    void storeToken_fromAuthResult_delegatesToStoreTokenString() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        User u = userAlice();
        LoginResult result = new LoginResult();
        result.setUser(u);
        result.setToken("fixed-token");
        when(valueOps.get("auth:user:u1")).thenReturn(null);

        authService.storeToken(result);

        verify(valueOps).set(eq("auth:token:fixed-token"), eq("u1"), eq(1L), eq(TimeUnit.DAYS));
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

    private static User userBob() {
        User user = new User();
        user.setId("u2");
        user.setName("Bob");
        user.setEmail("bob@example.com");
        user.setRole(Role.Customer);
        user.setAvatar(null);
        user.setPasswordHash("hashed_pw_2");
        return user;
    }
}

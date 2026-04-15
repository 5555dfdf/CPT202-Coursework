package org.example.coursework3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.coursework3.dto.request.CaptchaRequest;
import org.example.coursework3.dto.request.EmailLoginRequest;
import org.example.coursework3.dto.request.LoginRequest;
import org.example.coursework3.dto.request.RegisterRequest;
import org.example.coursework3.entity.Role;
import org.example.coursework3.entity.User;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.service.AliyunMailService;
import org.example.coursework3.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private AliyunMailService mailService;

    @Test
    void loginShouldReturnUserAndTokenAndStoreToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("hashed_pw_1");

        User user = buildAliceFromSql();
        when(authService.login(eq("alice@example.com"), eq("hashed_pw_1"))).thenReturn(user);
        doNothing().when(authService).storeToken(any());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("Customer"))
                .andExpect(jsonPath("$.data.token", notNullValue()));

        verify(authService).storeToken(any());
    }

    @Test
    void sendCaptchaShouldCallMailService() throws Exception {
        CaptchaRequest request = new CaptchaRequest();
        request.setEmail("alice@example.com");
        request.setScene("register");

        doNothing().when(mailService).sendCaptcha("alice@example.com");

        mockMvc.perform(post("/auth/send-email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("验证码已发送"));

        verify(mailService).sendCaptcha("alice@example.com");
    }

    @Test
    void sendCaptchaShouldReturnBadRequestWhenEmailMissing() throws Exception {
        CaptchaRequest request = new CaptchaRequest();
        request.setScene("register");

        mockMvc.perform(post("/auth/send-email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldReturnUserAndTokenAndStoreToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Bob");
        request.setEmail("bob@example.com");
        request.setPassword("newPlainPassword");
        request.setVerificationCode("123456");

        User user = buildBobFromSql();
        when(authService.register("Bob", "bob@example.com", "123456", "newPlainPassword")).thenReturn(user);
        doNothing().when(authService).storeToken(any());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.user.name").value("Bob"))
                .andExpect(jsonPath("$.data.user.email").value("bob@example.com"))
                .andExpect(jsonPath("$.data.token", notNullValue()));

        verify(authService).storeToken(any());
    }

    @Test
    void logoutShouldDeleteTokenFromHeader() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("已登出"));

        verify(authService).deleteToken("token-123");
    }

    @Test
    void loginByEmailCodeShouldReturnUserAndTokenAndStoreToken() throws Exception {
        EmailLoginRequest request = new EmailLoginRequest();
        request.setEmail("alice@example.com");
        request.setVerificationCode("123456");

        User user = buildAliceFromSql();
        when(authService.loginByCode("alice@example.com", "123456")).thenReturn(user);

        mockMvc.perform(post("/auth/login-by-email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.user.id").value("u1"))
                .andExpect(jsonPath("$.data.token", notNullValue()));

        verify(authService).storeToken(any());
    }

    @Test
    void loginShouldReturnUnauthorizedWhenServiceThrowsMsgException() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("wrong");

        when(authService.login("alice@example.com", "wrong")).thenThrow(new MsgException("密码不正确"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("密码不正确"));
    }

    // sql 测试用例 users：u1 Alice 
    private static User buildAliceFromSql() {
        User user = new User();
        user.setId("u1");
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setRole(Role.Customer);
        user.setAvatar(null);
        user.setPasswordHash("hashed_pw_1");
        return user;
    }

    // sql 测试用例 users：u2 Bob 
    private static User buildBobFromSql() {
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

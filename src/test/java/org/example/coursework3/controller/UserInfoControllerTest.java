package org.example.coursework3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.coursework3.dto.request.UpdateSelfInfoRequest;
import org.example.coursework3.entity.Role;
import org.example.coursework3.entity.User;
import org.example.coursework3.service.AuthService;
import org.example.coursework3.service.UpdateInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(UserInfoController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UpdateInfoService updateInfoService;

    @Test
    void getSelfInfoShouldResolveTokenAndReturnUser() throws Exception {
        User user = buildAliceFromSql();
        when(authService.getUserIdByToken("token-123")).thenReturn("u1");
        when(authService.getSelfInfo("u1")).thenReturn(user);

        mockMvc.perform(get("/me")
                        .header("Authorization", "Bearer token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value("u1"))
                .andExpect(jsonPath("$.data.name").value("Alice"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.role").value("Customer"));

        verify(authService).getUserIdByToken("token-123");
        verify(authService).getSelfInfo("u1");
    }

    @Test
    void updateSelfInfoShouldResolveTokenAndReturnUpdatedUser() throws Exception {
        UpdateSelfInfoRequest request = new UpdateSelfInfoRequest();
        request.setName("Alice");
        request.setAvatar("https://example.com/avatars/u1.png");

        User updatedUser = buildAliceFromSql();
        updatedUser.setAvatar("https://example.com/avatars/u1.png");

        when(authService.getUserIdByToken("token-123")).thenReturn("u1");
        when(updateInfoService.updateSelfInfo(eq("u1"), eq(request))).thenReturn(updatedUser);

        mockMvc.perform(patch("/me")
                        .header("Authorization", "Bearer token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.name").value("Alice"))
                .andExpect(jsonPath("$.data.avatar").value("https://example.com/avatars/u1.png"));

        verify(authService).getUserIdByToken("token-123");
        verify(updateInfoService).updateSelfInfo(eq("u1"), eq(request));
    }

    @Test
    void getSelfInfoShouldReturnBadRequestWhenAuthorizationHeaderMissing() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isBadRequest());
    }

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
}

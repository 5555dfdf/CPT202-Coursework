package org.example.coursework3.controller;


import lombok.extern.slf4j.Slf4j;
import org.example.coursework3.dto.request.ChangePasswordRequest;
import org.example.coursework3.dto.request.UpdateSelfInfoRequest;
import org.example.coursework3.result.Result;
import org.example.coursework3.dto.response.UserResult;
import org.example.coursework3.service.AuthService;
import org.example.coursework3.service.UpdateInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@Slf4j
public class UserInfoController {

    @Autowired
    private AuthService authService;
    @Autowired
    private UpdateInfoService updateInfoService;

    @GetMapping("/me")
    public Result<UserResult> getSelfInfo(@RequestHeader("Authorization") String authHeader){
        String token = authHeader.replace("Bearer ", "");
        String userId = authService.getUserIdByToken(token);
        UserResult result = new UserResult();
        return Result.success(result.toDTO(authService.getSelfInfo(userId)));
    }

    @PatchMapping("/me")
    public Result<UserResult> updateSelfInfo(@RequestHeader("Authorization") String authHeader, @RequestBody UpdateSelfInfoRequest request){
        String token = authHeader.replace("Bearer ", "");
        String userId = authService.getUserIdByToken(token);
        UserResult result = new UserResult();
        return Result.success(result.toDTO(updateInfoService.updateSelfInfo(userId, request)));
    }

    @PostMapping({"/me/change-password", "/me/password"})
    public Result<Void> changePassword(@RequestHeader("Authorization") String authHeader, @RequestBody ChangePasswordRequest request) {
        String token = authHeader.replace("Bearer ", "");
        String userId = authService.getUserIdByToken(token);

        String oldPassword = hasText(request.getOldPassword()) ? request.getOldPassword() : request.getCurrentPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        updateInfoService.changePassword(userId, oldPassword, newPassword, confirmPassword);
        return Result.success("Password changed successfully");
    }

    @PostMapping("/me/verify-password")
    public Result<Void> verifyPassword(@RequestHeader("Authorization") String authHeader, @RequestBody ChangePasswordRequest request) {
        String token = authHeader.replace("Bearer ", "");
        String userId = authService.getUserIdByToken(token);
        String oldPassword = hasText(request.getOldPassword()) ? request.getOldPassword() : request.getCurrentPassword();
        updateInfoService.verifyOldPassword(userId, oldPassword);
        return Result.success("Current password is correct");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

}

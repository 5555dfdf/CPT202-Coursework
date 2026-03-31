package org.example.courework3.controller;


import lombok.extern.slf4j.Slf4j;
import org.example.courework3.dto.UpdateSelfInfoRequest;
import org.example.courework3.result.Result;
import org.example.courework3.result.UserResult;
import org.example.courework3.service.AuthService;
import org.example.courework3.service.UpdateInfoService;
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
    public Result<UserResult> getSelfInfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.error("UNAUTHORIZED", "Missing or invalid Authorization header");
        }
        String token = authHeader.replace("Bearer ", "");
        String userId = authService.getUserIdByToken(token);
        UserResult result = new UserResult();
        return Result.success(result.toDTO(authService.getSelfInfo(userId)));
    }

    @PatchMapping("/me")
    public Result<UserResult> updateSelfInfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateSelfInfoRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.error("UNAUTHORIZED", "Missing or invalid Authorization header");
        }
        String token = authHeader.replace("Bearer ", "");
        String userId = authService.getUserIdByToken(token);
        UserResult result = new UserResult();
        return Result.success(result.toDTO(updateInfoService.updateSelfInfo(userId, request)));
    }

}

package org.example.coursework3.controller;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.dto.request.CreateSpecialistRequest;
import org.example.coursework3.dto.request.EditSpecialistRequest;
import org.example.coursework3.dto.request.UpdateSpecialistStatusRequest;
import org.example.coursework3.entity.Role;
import org.example.coursework3.entity.Specialist;
import org.example.coursework3.result.Result;
import org.example.coursework3.service.AdminService;
import org.example.coursework3.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private AuthService authService;

    // 1. 创建专家
    @PostMapping("/specialists")
    public Result<Specialist> createSpecialist(@RequestHeader("Authorization") String authHeader, @RequestBody CreateSpecialistRequest request) {
        String token = authHeader.replace("Bearer ", "");
        String userId = authService.getUserIdByToken(token);
        Role role = authService.getRoleByUserId(userId);
        if (role == Role.Admin) {
            return Result.success(adminService.createSpecialist(request));
        }
        return Result.error("ERROR","请以管理员身份创建");
    }

    // 2. 更新专家信息
    @PatchMapping("/specialists/{id}")
    public Result<Specialist> updateSpecialist(@RequestHeader("Authorization") String authHeader, @PathVariable String id, @RequestBody EditSpecialistRequest request) {
        String token = authHeader.replace("Bearer ", "");
        String userId = authService.getUserIdByToken(token);
        Role role = authService.getRoleByUserId(userId);
        if (role == Role.Admin) {
            return Result.success(adminService.updateSpecialist(id, request));
        }
        return Result.error("ERROR","请以管理员身份修改");

    }

    //3. 设置专家状态
    @PostMapping("/specialists/{id}/status")
    public Result<Specialist> updateSpecialistStatus(@RequestHeader("Authorization") String authHeader,
                                               @PathVariable String id,
                                               @RequestBody UpdateSpecialistStatusRequest request){
        String token = authHeader.replace("Bearer ", "");
        String userId = authService.getUserIdByToken(token);
        Role role = authService.getRoleByUserId(userId);
        if (role != Role.Admin) {
            return Result.error("ERROR", "请以管理员身份修改");
        }
        if (request == null || request.getStatus() == null) {
            return Result.error("ERROR", "status不能为空");
        }
        return Result.success(adminService.updateSpecialistStatus(id, request.getStatus()));
    }


    // 4. 删除专家
    @DeleteMapping("/specialists/{id}")
    public Result<Void> deleteSpecialist(@RequestHeader("Authorization") String authHeader, @PathVariable String id) {
        String token = authHeader.replace("Bearer ", "");
        String userId = authService.getUserIdByToken(token);
        Role role = authService.getRoleByUserId(userId);
        if (role != Role.Admin) {
            return Result.error("ERROR", "请以管理员身份修改");
        }
        adminService.deleteSpecialist(id);
    return Result.success("删除成功");
    }
//
//    // 5. 创建专长
//    @PostMapping("/expertise")
//    public ResponseEntity<?> createExpertise(@RequestBody Map<String, String> payload) {
//        try {
//            String name = payload.get("name");
//            String description = payload.get("description");
//            Expertise expertise = adminService.createExpertise(name, description);
//            return ResponseEntity.status(HttpStatus.CREATED).body(expertise);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
//        }
//    }
//
//    // 6. 更新专长
//    @PatchMapping("/expertise/{id}")
//    public ResponseEntity<?> updateExpertise(@PathVariable String id,
//                                             @RequestBody Map<String, String> payload) {
//        try {
//            String name = payload.get("name");
//            String description = payload.get("description");
//            Expertise expertise = adminService.updateExpertise(id, name, description);
//            return ResponseEntity.ok(expertise);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
//        }
//    }
//
//    // 7. 删除专长
//    @DeleteMapping("/expertise/{id}")
//    public ResponseEntity<?> deleteExpertise(@PathVariable String id) {
//        try {
//            adminService.deleteExpertise(id);
//            return ResponseEntity.ok(Map.of("message", "Expertise deleted successfully"));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
//        }
//    }
}
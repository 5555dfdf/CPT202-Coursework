package org.example.courework3.controller;

import lombok.RequiredArgsConstructor;
import org.example.courework3.entity.Expertise;
import org.example.courework3.entity.Specialist;
import org.example.courework3.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('Admin')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 1. 创建专家
    @PostMapping("/specialists")
    public ResponseEntity<?> createSpecialist(@RequestBody Map<String, Object> payload) {
        try {
            Specialist specialist = adminService.createSpecialist(payload);
            return ResponseEntity.status(HttpStatus.CREATED).body(specialist);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 2. 更新专家信息
    @PatchMapping("/specialists/{id}")
    public ResponseEntity<?> updateSpecialist(@PathVariable String id,
                                              @RequestBody Map<String, Object> payload) {
        try {
            Specialist specialist = adminService.updateSpecialist(id, payload);
            return ResponseEntity.ok(specialist);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 3. 设置专家状态
    @PostMapping("/specialists/{id}/status")
    public ResponseEntity<?> setSpecialistStatus(@PathVariable String id,
                                                 @RequestBody Map<String, String> payload) {
        try {
            String status = payload.get("status");
            Specialist specialist = adminService.setSpecialistStatus(id, status);
            return ResponseEntity.ok(specialist);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 4. 删除专家
    @DeleteMapping("/specialists/{id}")
    public ResponseEntity<?> deleteSpecialist(@PathVariable String id) {
        try {
            adminService.deleteSpecialist(id);
            return ResponseEntity.ok(Map.of("message", "Specialist deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 5. 创建专长
    @PostMapping("/expertise")
    public ResponseEntity<?> createExpertise(@RequestBody Map<String, String> payload) {
        try {
            String name = payload.get("name");
            String description = payload.get("description");
            Expertise expertise = adminService.createExpertise(name, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(expertise);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 6. 更新专长
    @PatchMapping("/expertise/{id}")
    public ResponseEntity<?> updateExpertise(@PathVariable String id,
                                             @RequestBody Map<String, String> payload) {
        try {
            String name = payload.get("name");
            String description = payload.get("description");
            Expertise expertise = adminService.updateExpertise(id, name, description);
            return ResponseEntity.ok(expertise);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 7. 删除专长
    @DeleteMapping("/expertise/{id}")
    public ResponseEntity<?> deleteExpertise(@PathVariable String id) {
        try {
            adminService.deleteExpertise(id);
            return ResponseEntity.ok(Map.of("message", "Expertise deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
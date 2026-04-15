package org.example.coursework3.controller;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.dto.request.*;
import org.example.coursework3.dto.response.BookingPageResult;
import org.example.coursework3.entity.Expertise;
import org.example.coursework3.entity.Specialist;
import org.example.coursework3.result.Result;
import org.example.coursework3.service.AdminService;
import org.example.coursework3.service.AuthService;
import org.example.coursework3.service.CustomerBookingService;
import org.example.coursework3.vo.AdminSlotVo;
import org.example.coursework3.vo.SingleBookingVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private AuthService authService;
    @Autowired
    private CustomerBookingService customerBookingService;

    // 1. 创建专家
    @PostMapping("/specialists")
    public Result<Specialist> createSpecialist(@RequestHeader("Authorization") String authHeader, @RequestBody CreateSpecialistRequest request) {
        if (authService.verifyAsAdmin(authHeader)) {
            return Result.success(adminService.createSpecialist(request));
        }
        return Result.error("ERROR","请以管理员身份创建");
    }

    // 2. 更新专家信息
    @PatchMapping("/specialists/{id}")
    public Result<Specialist> updateSpecialist(@RequestHeader("Authorization") String authHeader, @PathVariable String id, @RequestBody EditSpecialistRequest request) {
        if (authService.verifyAsAdmin(authHeader)) {
            return Result.success(adminService.updateSpecialist(id, request));
        }
        return Result.error("ERROR","请以管理员身份修改");

    }

    //3. 设置专家状态
    @PostMapping("/specialists/{id}/status")
    public Result<Specialist> updateSpecialistStatus(@RequestHeader("Authorization") String authHeader,
                                               @PathVariable String id,
                                               @RequestBody UpdateSpecialistStatusRequest request){
        if (!authService.verifyAsAdmin(authHeader)) {
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
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "请以管理员身份修改");
        }
        adminService.deleteSpecialist(id);
    return Result.success("删除成功");
    }

    // 5. 创建专长
    @PostMapping("/expertise")
    public Result<Expertise> createExpertise(@RequestHeader("Authorization") String authHeader, @RequestBody ExpertiseRequest request){
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "请以管理员身份修改");
        }
        return Result.success(adminService.createExpertise(request.getName(), request.getDescription()));
    }

    // 6. 更新专长
    @PatchMapping("/expertise/{id}")
    public Result<Expertise> updateExpertiseInfo(@RequestHeader("Authorization") String authHeader, @PathVariable String id, @RequestBody ExpertiseRequest request){
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "请以管理员身份修改");
        }
        return Result.success(adminService.updateExpertise(id, request.getName(), request.getDescription()));
    }

    // 7. 删除专长
    @DeleteMapping("/expertise/{id}")
    public Result<Void> deleteExpertise(@RequestHeader("Authorization") String authHeader, @PathVariable String id){
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "请以管理员身份修改");
        }
        adminService.deleteExpertise(id);
        return Result.success("专长删除成功");
    }

    @GetMapping("/slots")
    public Result<List<AdminSlotVo>> listSlots(@RequestHeader("Authorization") String authHeader,
                                               @RequestParam(required = false) String specialistId,
                                               @RequestParam(required = false) String date,
                                               @RequestParam(required = false) String from,
                                               @RequestParam(required = false) String to,
                                               @RequestParam(required = false) Boolean available) {
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "please use admin role");
        }
        return Result.success(adminService.listSlots(specialistId, date, from, to, available));
    }

    @PostMapping("/slots")
    public Result<AdminSlotVo> createSlot(@RequestHeader("Authorization") String authHeader,
                                          @RequestBody SlotRequest request) {
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "please use admin role");
        }
        return Result.success(adminService.createSlot(request.getSpecialistId(), request.getDate(), request.getStart(), request.getEnd(), request.getAvailable()));
    }

    @PatchMapping("/slots/{id}")
    public Result<AdminSlotVo> updateSlot(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable String id,
                                          @RequestBody SlotRequest request) {
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "please use admin role");
        }
        return Result.success(adminService.updateSlot(id, request));
    }

    @DeleteMapping("/slots/{id}")
    public Result<Void> deleteSlot(@RequestHeader("Authorization") String authHeader, @PathVariable String id) {
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "please use admin role");
        }
        adminService.deleteSlot(id);
        return Result.success("slot deleted successfully");
    }

    @GetMapping("/bookings")
    public Result<BookingPageResult> listBookings(@RequestHeader("Authorization") String authHeader,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "please use admin role");
        }
        return Result.success(adminService.listBookings(page, pageSize));
    }

    @GetMapping("/bookings/{id}")
    public Result<SingleBookingVo> getBooking(@RequestHeader("Authorization") String authHeader,
                                              @PathVariable String id) {
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "please use admin role");
        }
        return Result.success(customerBookingService.getSingleBookingInfo(id));
    }

}
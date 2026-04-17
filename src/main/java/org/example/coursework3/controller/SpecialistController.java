package org.example.coursework3.controller;

import org.example.coursework3.dto.request.SlotRequest;
import org.example.coursework3.dto.response.BookingPageResult;
import org.example.coursework3.dto.response.BookingActionResult;
import org.example.coursework3.dto.request.RejectRequest;
import org.example.coursework3.result.Result;
import org.example.coursework3.service.AdminService;
import org.example.coursework3.service.AuthService;
import org.example.coursework3.service.SpecialistBookingService;
import org.example.coursework3.vo.AdminSlotVo;
import org.example.coursework3.vo.SingleBookingVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/specialist")
@CrossOrigin
public class SpecialistController {

    @Autowired
    private SpecialistBookingService bookingService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminService adminService;

    @GetMapping("/booking-requests")
    public Result<BookingPageResult> getBookingRequests(@RequestHeader("Authorization") String authHeader,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        BookingPageResult pageResult = bookingService.getSpecialistBookings(authHeader, status, page, pageSize);
        return Result.success(pageResult);
    }

    @PostMapping("/bookings/{id}/confirm")
    public Result<BookingActionResult> confirmBooking(@RequestHeader("Authorization") String authHeader,
                                                @PathVariable("id") String bookingId) {
        BookingActionResult actionResult = bookingService.confirmBooking(authHeader, bookingId);
        return Result.success(actionResult);
    }

    @PostMapping("/bookings/{id}/reject")
    public Result<BookingActionResult> rejectBooking(@RequestHeader("Authorization") String authHeader,
                                              @PathVariable("id") String bookingId,
                                              @RequestBody(required = false)RejectRequest rejectRequest) {
        String reason = rejectRequest != null? rejectRequest.getReason() : null;
        BookingActionResult actionResult = bookingService.rejectBooking(authHeader,bookingId, reason);
        return Result.success(actionResult);
    }
    @PostMapping("bookings/{id}/complete")
    public Result<BookingActionResult> completeBooking(@RequestHeader("Authorization") String authHeader,
                                                  @PathVariable("id") String bookingId){
        BookingActionResult actionResult = bookingService.completeBooking(authHeader,bookingId);
        return Result.success(actionResult);
    }

    @GetMapping("bookings/{id}")
    public Result<SingleBookingVo> getSingleBookingInfo(@RequestHeader("Authorization") String authHeader, @PathVariable String id){
        if (!authService.verifyAsSpecialist(authHeader)) {
            return Result.error("ERROR", "请以专家身份查看");
        }
        return Result.success(bookingService.getSingleBookingInfo(id));
    }
    @PostMapping("/slots")
    public Result<AdminSlotVo> createSlot(@RequestHeader("Authorization") String authHeader,
                                          @RequestBody SlotRequest request) {
        if (!authService.verifyAsSpecialist(authHeader)) {
            return Result.error("ERROR", "please use Specialist role");
        }
        request.setSpecialistId(authService.getUserIdByAuth(authHeader));
        return Result.success(adminService.createSlot(request));
    }

    @GetMapping("/slots")
    public Result<List<AdminSlotVo>> listSlots(@RequestHeader("Authorization") String authHeader,
                                               @RequestParam(required = false) String date,
                                               @RequestParam(required = false) String from,
                                               @RequestParam(required = false) String to,
                                               @RequestParam(required = false) Boolean available) {
        if (!authService.verifyAsSpecialist(authHeader)) {
            return Result.error("ERROR", "please use Specialist role");
        }
        String specialistId = authService.getUserIdByAuth(authHeader);
        return Result.success(adminService.listSlots(specialistId, date, from, to, available));
    }

    @DeleteMapping("/slots/{id}")
    public Result<Void> deleteSlot(@RequestHeader("Authorization") String authHeader, @PathVariable String id) {
        if (!authService.verifyAsSpecialist(authHeader)) {
            return Result.error("ERROR", "please use Specialist role");
        }
        adminService.deleteSlot(id);
        return Result.success("slot deleted successfully");
    }

    @PatchMapping("/slots/{id}")
    public Result<AdminSlotVo> updateSlot(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable String id,
                                          @RequestBody SlotRequest request) {
        if (!authService.verifyAsSpecialist(authHeader)) {
            return Result.error("ERROR", "please use Specialist role");
        }
        return Result.success(adminService.updateSlot(id, request));
    }

    @GetMapping("/slots/{id}")
    public Result<AdminSlotVo> getSingleSlotInfo(@RequestHeader("Authorization") String authHeader,
                                                 @PathVariable String id){
        if (!authService.verifyAsSpecialist(authHeader)) {
            return Result.error("ERROR", "please use Specialist role");
        }
        return Result.success(adminService.getSingleSlotInfo(id));
    }


}


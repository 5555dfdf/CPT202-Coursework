package org.example.coursework3.controller;

import org.example.coursework3.dto.response.BookingPageResult;
import org.example.coursework3.dto.response.BookingActionResult;
import org.example.coursework3.dto.request.RejectRequest;
import org.example.coursework3.result.Result;
import org.example.coursework3.service.SpecialistBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/specialist")
@CrossOrigin
public class SpecialistController {

    @Autowired
    private SpecialistBookingService bookingService;

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


}


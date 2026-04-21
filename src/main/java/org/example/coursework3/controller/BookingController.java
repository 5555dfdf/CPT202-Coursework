package org.example.coursework3.controller;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.dto.request.CreateBookingRequest;
import org.example.coursework3.dto.request.CreateBookingPaymentRequest;
import org.example.coursework3.dto.request.ConfirmBookingPaymentRequest;
import org.example.coursework3.dto.request.RescheduleBookingRequest;
import org.example.coursework3.dto.response.BookingActionResult;
import org.example.coursework3.dto.response.BookingPageResult;
import org.example.coursework3.dto.response.ConfirmBookingPaymentResult;
import org.example.coursework3.dto.response.CreateBookingPaymentResult;
import org.example.coursework3.dto.response.CreateBookingResult;
import org.example.coursework3.dto.response.RescheduleBookingResult;
import org.example.coursework3.dto.response.UnpaidPaymentItemResult;
import org.example.coursework3.dto.response.UnpaidPaymentsResult;
import org.example.coursework3.entity.BookingStatus;
import org.example.coursework3.result.Result;
import org.example.coursework3.service.AuthService;
import org.example.coursework3.service.CustomerBookingService;
import org.example.coursework3.vo.SingleBookingVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    @Autowired
    private CustomerBookingService bookingService;
    @Autowired
    private AuthService authService;

    @PostMapping
    public Result<CreateBookingResult> createBooking(@RequestHeader("Authorization") String authHeader, @RequestBody CreateBookingRequest request){
        if (!authService.verifyAsCustomer(authHeader)) {
            return Result.error("ERROR", "请以顾客身份修改");
        }
        return Result.success(bookingService.creatBooking(authService.getUserIdByAuth(authHeader), request));
    }

    @GetMapping
    public Result<BookingPageResult> getMyBookings(@RequestHeader("Authorization") String authHeader,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) String from,
                                                   @RequestParam(required = false) String to){
        if (!authService.verifyAsCustomer(authHeader)) {
            return Result.error("ERROR", "请以顾客身份查看");
        }
        String userId = authService.getUserIdByAuth(authHeader);
        BookingPageResult pageResult = bookingService.getMyBookings(userId, status, page, pageSize, from, to);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<SingleBookingVo> getSingleBookingInfo(@RequestHeader("Authorization") String authHeader, @PathVariable String id){
        if (!authService.verifyAsCustomer(authHeader)) {
            return Result.error("ERROR", "请以顾客身份查看");
        }
        return Result.success(bookingService.getSingleBookingInfo(id));
    }
    //cancel bookings
    @PostMapping("/{id}/cancel")
    public Result<BookingActionResult> cancelBooking(@RequestHeader("Authorization") String authHeader, @PathVariable String id){
        if (!authService.verifyAsCustomer(authHeader)) {
            return Result.error("ERROR", "请以顾客身份查看");
        }
        return Result.success(bookingService.cancelBooking(id));
    }
    // reschedule bookings
    @PostMapping("/{id}/reschedule")
    public Result<RescheduleBookingResult> rescheduleBooking(@RequestHeader("Authorization") String authHeader, @PathVariable String id,
                                                             @RequestBody RescheduleBookingRequest request){
        if (!authService.verifyAsCustomer(authHeader)) {
            return Result.error("ERROR", "请以顾客身份查看");
        }
        bookingService.rescheduleBooking(id, request.getSlotId());
        return Result.success(new RescheduleBookingResult(id, BookingStatus.Pending, request.getSlotId()));
    }

    @PostMapping("/{id}/payment")
    public Result<CreateBookingPaymentResult> createBookingPayment(@RequestHeader("Authorization") String authHeader,
                                                                   @PathVariable String id,
                                                                   @RequestBody(required = false) CreateBookingPaymentRequest request) {
        if (!authService.verifyAsCustomer(authHeader)) {
            return Result.error("ERROR", "请以顾客身份支付");
        }
        String userId = authService.getUserIdByAuth(authHeader);
        return Result.success(bookingService.createBookingPayment(userId, id, request));
    }

    @PostMapping("/{id}/payment/confirm")
    public Result<ConfirmBookingPaymentResult> confirmBookingPayment(@RequestHeader("Authorization") String authHeader,
                                                                     @PathVariable String id,
                                                                     @RequestBody(required = false) ConfirmBookingPaymentRequest request) {
        if (!authService.verifyAsCustomer(authHeader)) {
            return Result.error("ERROR", "请以顾客身份支付");
        }
        String userId = authService.getUserIdByAuth(authHeader);
        return Result.success(bookingService.confirmBookingPayment(userId, id, request));
    }

    @PostMapping("/{id}/payment/mock-success")
    public Result<ConfirmBookingPaymentResult> mockSuccessPayment(@RequestHeader("Authorization") String authHeader,
                                                                  @PathVariable String id) {
        if (!authService.verifyAsCustomer(authHeader)) {
            return Result.error("ERROR", "请以顾客身份支付");
        }
        String userId = authService.getUserIdByAuth(authHeader);
        return Result.success(bookingService.mockSuccessPayment(userId, id));
    }

    @PostMapping("/alipay/notify")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        boolean ok = bookingService.handleAlipayNotify(params);
        return ok ? "success" : "failure";
    }

    @GetMapping("/unpaid-payments")
    public Result<UnpaidPaymentsResult> listUnpaidPayments(@RequestHeader("Authorization") String authHeader) {
        if (!authService.verifyAsCustomer(authHeader)) {
            return Result.error("ERROR", "请以顾客身份查看");
        }
        String userId = authService.getUserIdByAuth(authHeader);
        return Result.success(bookingService.listUnpaidPayments(userId));
    }

    @GetMapping("/unpaid-payments/{id}")
    public Result<UnpaidPaymentItemResult> getUnpaidPayment(@RequestHeader("Authorization") String authHeader,
                                                            @PathVariable String id) {
        if (!authService.verifyAsCustomer(authHeader)) {
            return Result.error("ERROR", "请以顾客身份查看");
        }
        String userId = authService.getUserIdByAuth(authHeader);
        return Result.success(bookingService.getUnpaidPayment(userId, id));
    }

    @PostMapping("/unpaid-payments/{id}/resume")
    public Result<CreateBookingPaymentResult> resumeUnpaidPayment(@RequestHeader("Authorization") String authHeader,
                                                                  @PathVariable String id) {
        if (!authService.verifyAsCustomer(authHeader)) {
            return Result.error("ERROR", "请以顾客身份支付");
        }
        String userId = authService.getUserIdByAuth(authHeader);
        return Result.success(bookingService.resumeUnpaidPayment(userId, id));
    }
}

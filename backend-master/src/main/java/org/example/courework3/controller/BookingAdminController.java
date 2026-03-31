package org.example.courework3.controller;

import lombok.RequiredArgsConstructor;
import org.example.courework3.entity.Booking;
import org.example.courework3.entity.Specialist;
import org.example.courework3.entity.User;
import org.example.courework3.repository.SpecialistRepository;
import org.example.courework3.repository.UserRepository;
import org.example.courework3.result.Result;
import org.example.courework3.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/bookings")
@CrossOrigin
@PreAuthorize("hasRole('Admin')")
@RequiredArgsConstructor
public class BookingAdminController {

    private final BookingService bookingService;
    private final SpecialistRepository specialistRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> listBookings() {
        try {
            List<Booking> bookings = bookingService.listBookings(Map.of());
            List<Map<String, Object>> items = bookings.stream().map(this::toMap).toList();
            return ResponseEntity.ok(Result.success(Map.of(
                    "items", items,
                    "total", items.size()
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("FETCH_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBooking(@PathVariable String id) {
        try {
            Booking booking = bookingService.getBooking(id);
            return ResponseEntity.ok(Result.success(toMap(booking)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("NOT_FOUND", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(Booking booking) {
        String customerName = null;
        String specialistName = null;

        try {
            User customer = userRepository.findById(booking.getCustomerId()).orElse(null);
            if (customer != null) customerName = customer.getName();
        } catch (Exception ignored) {}

        try {
            Specialist specialist = specialistRepository.findById(booking.getSpecialistId()).orElse(null);
            if (specialist != null) specialistName = specialist.getName();
        } catch (Exception ignored) {}

        return Map.ofEntries(
                Map.entry("id", booking.getId() != null ? booking.getId() : ""),
                Map.entry("customerId", booking.getCustomerId() != null ? booking.getCustomerId() : ""),
                Map.entry("customerName", customerName != null ? customerName : ""),
                Map.entry("specialistId", booking.getSpecialistId() != null ? booking.getSpecialistId() : ""),
                Map.entry("specialistName", specialistName != null ? specialistName : ""),
                Map.entry("slotId", booking.getSlotId() != null ? booking.getSlotId() : ""),
                Map.entry("note", booking.getNote() != null ? booking.getNote() : ""),
                Map.entry("status", booking.getStatus() != null ? booking.getStatus() : ""),
                Map.entry("createdAt", booking.getCreatedAt() != null ? booking.getCreatedAt().toString() : ""),
                Map.entry("updatedAt", booking.getUpdatedAt() != null ? booking.getUpdatedAt().toString() : "")
        );
    }
}

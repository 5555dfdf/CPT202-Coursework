package org.example.courework3.controller;

import lombok.RequiredArgsConstructor;
import org.example.courework3.entity.Booking;
import org.example.courework3.entity.Slot;
import org.example.courework3.entity.Specialist;
import org.example.courework3.repository.BookingRepository;
import org.example.courework3.repository.SlotRepository;
import org.example.courework3.repository.SpecialistRepository;
import org.example.courework3.result.Result;
import org.example.courework3.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.example.courework3.entity.User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bookings")
@CrossOrigin
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final SpecialistRepository specialistRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listMyBookings(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status) {
        try {
            List<Booking> bookings = bookingRepository.findByCustomerIdOrderByCreatedAtDesc(user.getId());
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getBooking(@PathVariable String id,
                                        @AuthenticationPrincipal User user) {
        try {
            Booking booking = bookingRepository.findById(id).orElse(null);
            if (booking == null) {
                return ResponseEntity.status(404).body(Result.error("NOT_FOUND", "Booking not found"));
            }
            return ResponseEntity.ok(Result.success(toMap(booking)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("FETCH_ERROR", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Customer','Admin')")
    public ResponseEntity<?> createBooking(@AuthenticationPrincipal User user,
                                          @RequestBody Map<String, Object> payload) {
        try {
            Booking booking = bookingService.createBooking(payload, user.getId());
            return ResponseEntity.ok(Result.success(toMap(booking)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("CREATE_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelBooking(@PathVariable String id,
                                          @AuthenticationPrincipal User user,
                                          @RequestBody(required = false) Map<String, String> payload) {
        try {
            Booking booking = bookingRepository.findById(id).orElse(null);
            if (booking == null) {
                return ResponseEntity.status(404).body(Result.error("NOT_FOUND", "Booking not found"));
            }
            if (!booking.getCustomerId().equals(user.getId()) && !"Admin".equals(user.getRole().name())) {
                return ResponseEntity.status(403).body(Result.error("FORBIDDEN", "Not your booking"));
            }
            String reason = payload != null ? payload.get("reason") : null;
            Booking updated = bookingService.updateBookingStatus(id, "Cancelled", reason);
            return ResponseEntity.ok(Result.success(toMap(updated)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("CANCEL_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> rescheduleBooking(@PathVariable String id,
                                               @AuthenticationPrincipal User user,
                                               @RequestBody Map<String, String> payload) {
        try {
            Booking booking = bookingRepository.findById(id).orElse(null);
            if (booking == null) {
                return ResponseEntity.status(404).body(Result.error("NOT_FOUND", "Booking not found"));
            }
            if (!booking.getCustomerId().equals(user.getId()) && !"Admin".equals(user.getRole().name())) {
                return ResponseEntity.status(403).body(Result.error("FORBIDDEN", "Not your booking"));
            }
            String newSlotId = payload.get("slotId");
            if (newSlotId == null || newSlotId.isBlank()) {
                return ResponseEntity.badRequest().body(Result.error("INVALID", "slotId is required"));
            }
            Slot newSlot = slotRepository.findById(newSlotId).orElse(null);
            if (newSlot == null) {
                return ResponseEntity.badRequest().body(Result.error("INVALID", "Slot not found"));
            }
            if (!Boolean.TRUE.equals(newSlot.getAvailable())) {
                return ResponseEntity.badRequest().body(Result.error("INVALID", "Slot is not available"));
            }
            Slot oldSlot = slotRepository.findById(booking.getSlotId()).orElse(null);
            if (oldSlot != null) {
                oldSlot.setAvailable(true);
                slotRepository.save(oldSlot);
            }
            booking.setSlotId(newSlotId);
            booking.setStatus("Rescheduled");
            Booking updated = bookingRepository.save(booking);
            newSlot.setAvailable(false);
            slotRepository.save(newSlot);
            return ResponseEntity.ok(Result.success(toMap(updated)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("RESCHEDULE_ERROR", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(Booking booking) {
        String specialistName = "";
        try {
            Specialist specialist = specialistRepository.findById(booking.getSpecialistId()).orElse(null);
            if (specialist != null) specialistName = specialist.getName();
        } catch (Exception ignored) {}

        String timeStr = "";
        try {
            Slot slot = slotRepository.findById(booking.getSlotId()).orElse(null);
            if (slot != null) {
                timeStr = slot.getStartTime().toString();
            }
        } catch (Exception ignored) {}

        return Map.ofEntries(
                Map.entry("id", booking.getId() != null ? booking.getId() : ""),
                Map.entry("customerId", booking.getCustomerId() != null ? booking.getCustomerId() : ""),
                Map.entry("specialistId", booking.getSpecialistId() != null ? booking.getSpecialistId() : ""),
                Map.entry("specialistName", specialistName),
                Map.entry("slotId", booking.getSlotId() != null ? booking.getSlotId() : ""),
                Map.entry("time", timeStr),
                Map.entry("startTime", timeStr),
                Map.entry("note", booking.getNote() != null ? booking.getNote() : ""),
                Map.entry("status", booking.getStatus() != null ? booking.getStatus() : ""),
                Map.entry("createdAt", booking.getCreatedAt() != null ? booking.getCreatedAt().toString() : ""),
                Map.entry("updatedAt", booking.getUpdatedAt() != null ? booking.getUpdatedAt().toString() : "")
        );
    }
}

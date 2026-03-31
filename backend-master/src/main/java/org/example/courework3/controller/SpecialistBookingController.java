package org.example.courework3.controller;

import lombok.RequiredArgsConstructor;
import org.example.courework3.entity.Booking;
import org.example.courework3.entity.BookingHistory;
import org.example.courework3.entity.Specialist;
import org.example.courework3.entity.User;
import org.example.courework3.repository.BookingHistoryRepository;
import org.example.courework3.repository.BookingRepository;
import org.example.courework3.repository.SlotRepository;
import org.example.courework3.repository.SpecialistRepository;
import org.example.courework3.result.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/specialist")
@CrossOrigin
@RequiredArgsConstructor
public class SpecialistBookingController {

    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final SpecialistRepository specialistRepository;
    private final SlotRepository slotRepository;

    @GetMapping("/booking-requests")
    @PreAuthorize("hasRole('Specialist')")
    public ResponseEntity<?> listBookingRequests(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status) {
        try {
            Specialist specialist = specialistRepository.findById(user.getId()).orElse(null);
            if (specialist == null) {
                return ResponseEntity.status(403).body(Result.error("FORBIDDEN", "You are not registered as a specialist"));
            }
            List<Booking> bookings = bookingRepository.findBySpecialistIdOrderByCreatedAtDesc(user.getId());
            List<Map<String, Object>> items = bookings.stream()
                    .filter(b -> status == null || status.isBlank() || status.equals(b.getStatus()))
                    .map(this::toMap)
                    .toList();
            return ResponseEntity.ok(Result.success(Map.of(
                    "items", items,
                    "total", items.size()
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("FETCH_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/bookings/{id}/confirm")
    @PreAuthorize("hasRole('Specialist')")
    public ResponseEntity<?> confirmBooking(@PathVariable String id,
                                          @AuthenticationPrincipal User user) {
        try {
            Booking booking = bookingRepository.findById(id).orElse(null);
            if (booking == null) {
                return ResponseEntity.badRequest().body(Result.error("NOT_FOUND", "Booking not found"));
            }
            if (!booking.getSpecialistId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Result.error("FORBIDDEN", "Not your booking"));
            }
            booking.setStatus("Confirmed");
            Booking saved = bookingRepository.save(booking);
            bookingHistoryRepository.save(makeHistory(id, "Confirmed"));
            return ResponseEntity.ok(Result.success(toMap(saved)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("ERROR", e.getMessage()));
        }
    }

    @PostMapping("/bookings/{id}/reject")
    @PreAuthorize("hasRole('Specialist')")
    public ResponseEntity<?> rejectBooking(@PathVariable String id,
                                         @AuthenticationPrincipal User user,
                                         @RequestBody(required = false) Map<String, String> payload) {
        try {
            Booking booking = bookingRepository.findById(id).orElse(null);
            if (booking == null) {
                return ResponseEntity.badRequest().body(Result.error("NOT_FOUND", "Booking not found"));
            }
            if (!booking.getSpecialistId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Result.error("FORBIDDEN", "Not your booking"));
            }
            String reason = payload != null ? payload.get("reason") : null;
            booking.setStatus("Rejected");
            Booking saved = bookingRepository.save(booking);
            bookingHistoryRepository.save(makeHistory(id, "Rejected", reason));
            releaseSlot(booking.getSlotId());
            return ResponseEntity.ok(Result.success(toMap(saved)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("ERROR", e.getMessage()));
        }
    }

    @PostMapping("/bookings/{id}/complete")
    @PreAuthorize("hasRole('Specialist')")
    public ResponseEntity<?> completeBooking(@PathVariable String id,
                                             @AuthenticationPrincipal User user) {
        try {
            Booking booking = bookingRepository.findById(id).orElse(null);
            if (booking == null) {
                return ResponseEntity.badRequest().body(Result.error("NOT_FOUND", "Booking not found"));
            }
            if (!booking.getSpecialistId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Result.error("FORBIDDEN", "Not your booking"));
            }
            booking.setStatus("Completed");
            Booking saved = bookingRepository.save(booking);
            bookingHistoryRepository.save(makeHistory(id, "Completed"));
            return ResponseEntity.ok(Result.success(toMap(saved)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("ERROR", e.getMessage()));
        }
    }

    private BookingHistory makeHistory(String bookingId, String status) {
        return makeHistory(bookingId, status, null);
    }

    private BookingHistory makeHistory(String bookingId, String status, String reason) {
        BookingHistory h = new BookingHistory();
        h.setBookingId(bookingId);
        h.setStatus(status);
        h.setReason(reason);
        return h;
    }

    private void releaseSlot(String slotId) {
        if (slotId == null || slotId.isBlank()) return;
        slotRepository.findById(slotId).ifPresent(slot -> {
            slot.setAvailable(true);
            slotRepository.save(slot);
        });
    }

    private Map<String, Object> toMap(Booking b) {
        return Map.ofEntries(
                Map.entry("id", b.getId() != null ? b.getId() : ""),
                Map.entry("customerId", b.getCustomerId() != null ? b.getCustomerId() : ""),
                Map.entry("specialistId", b.getSpecialistId() != null ? b.getSpecialistId() : ""),
                Map.entry("slotId", b.getSlotId() != null ? b.getSlotId() : ""),
                Map.entry("note", b.getNote() != null ? b.getNote() : ""),
                Map.entry("status", b.getStatus() != null ? b.getStatus() : ""),
                Map.entry("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : ""),
                Map.entry("updatedAt", b.getUpdatedAt() != null ? b.getUpdatedAt().toString() : "")
        );
    }
}

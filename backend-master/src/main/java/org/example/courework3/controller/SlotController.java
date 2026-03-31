package org.example.courework3.controller;

import lombok.RequiredArgsConstructor;
import org.example.courework3.entity.Slot;
import org.example.courework3.result.Result;
import org.example.courework3.service.SlotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/slots")
@CrossOrigin
@PreAuthorize("hasRole('Admin')")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    @GetMapping
    public ResponseEntity<?> listSlots(
            @RequestParam(required = false) String specialistId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String available) {
        try {
            Map<String, Object> params = Map.of(
                    "specialistId", specialistId != null ? specialistId : "",
                    "date", date != null ? date : "",
                    "from", from != null ? from : "",
                    "to", to != null ? to : "",
                    "available", available != null ? available : ""
            );
            List<Slot> slots = slotService.listSlots(params);
            List<Map<String, Object>> items = slots.stream().map(this::toMap).toList();
            return ResponseEntity.ok(Result.success(Map.of(
                    "items", items,
                    "total", items.size()
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("FETCH_ERROR", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createSlot(@RequestBody Map<String, Object> payload) {
        try {
            Slot slot = slotService.createSlot(payload);
            return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(toMap(slot)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("CREATE_ERROR", e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateSlot(@PathVariable String id,
                                       @RequestBody Map<String, Object> payload) {
        try {
            Slot slot = slotService.updateSlot(id, payload);
            return ResponseEntity.ok(Result.success(toMap(slot)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("UPDATE_ERROR", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSlot(@PathVariable String id) {
        try {
            slotService.deleteSlot(id);
            return ResponseEntity.ok(Result.success(Map.of("message", "Slot deleted successfully")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("DELETE_ERROR", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(Slot slot) {
        return Map.of(
                "id", slot.getId() != null ? slot.getId() : "",
                "specialistId", slot.getSpecialistId() != null ? slot.getSpecialistId() : "",
                "startTime", slot.getStartTime() != null ? slot.getStartTime().toString() : "",
                "endTime", slot.getEndTime() != null ? slot.getEndTime().toString() : "",
                "date", slot.getStartTime() != null ? slot.getStartTime().toLocalDate().toString() : "",
                "start", slot.getStartTime() != null ? slot.getStartTime().toLocalTime().toString() : "",
                "end", slot.getEndTime() != null ? slot.getEndTime().toLocalTime().toString() : "",
                "available", slot.getAvailable() != null ? slot.getAvailable() : true,
                "createdAt", slot.getCreatedAt() != null ? slot.getCreatedAt().toString() : "",
                "updatedAt", slot.getUpdatedAt() != null ? slot.getUpdatedAt().toString() : ""
        );
    }
}

package org.example.courework3.controller;

import lombok.RequiredArgsConstructor;
import org.example.courework3.entity.Slot;
import org.example.courework3.entity.Specialist;
import org.example.courework3.repository.SlotRepository;
import org.example.courework3.repository.SpecialistRepository;
import org.example.courework3.result.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/specialists")
@CrossOrigin
@RequiredArgsConstructor
public class SpecialistController {

    private final SpecialistRepository specialistRepository;
    private final SlotRepository slotRepository;

    @GetMapping("/{id}/slots")
    public Result<List<Map<String, Object>>> listSpecialistSlots(
            @PathVariable String id,
            @RequestParam(required = false) String date) {
        try {
            List<Slot> slots;
            if (date != null && !date.isBlank()) {
                LocalDate targetDate = LocalDate.parse(date);
                slots = slotRepository.findBySpecialistIdOrderByStartTimeAsc(id).stream()
                        .filter(s -> s.getStartTime().toLocalDate().isEqual(targetDate))
                        .toList();
            } else {
                slots = slotRepository.findByAvailableTrueAndSpecialistIdOrderByStartTimeAsc(id);
            }
            List<Map<String, Object>> items = slots.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getAvailable()))
                    .map(s -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", s.getId() != null ? s.getId() : "");
                        map.put("specialistId", s.getSpecialistId() != null ? s.getSpecialistId() : "");
                        map.put("startTime", s.getStartTime() != null ? s.getStartTime().toString() : "");
                        map.put("endTime", s.getEndTime() != null ? s.getEndTime().toString() : "");
                        map.put("available", s.getAvailable() != null ? s.getAvailable() : true);
                        return map;
                    })
                    .toList();
            return Result.success(items);
        } catch (Exception e) {
            return Result.error("FETCH_ERROR", "Failed to load slots: " + e.getMessage());
        }
    }

    @GetMapping
    public Result<Map<String, Object>> listSpecialists(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            List<Specialist> allSpecialists = specialistRepository.findAll();

            int total = allSpecialists.size();
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, total);

            List<Specialist> pageData = start < total
                    ? allSpecialists.subList(start, end)
                    : List.of();

            List<Map<String, Object>> items = pageData.stream()
                    .map(this::toMap)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("items", items);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);

            return Result.success(result);
        } catch (Exception e) {
            return Result.error("FETCH_ERROR", "Failed to load specialists: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getSpecialist(@PathVariable String id) {
        try {
            Specialist specialist = specialistRepository.findById(id)
                    .orElse(null);

            if (specialist == null) {
                return Result.success(Map.of("message", "Specialist not found"));
            }

            return Result.success(toMap(specialist));
        } catch (Exception e) {
            return Result.error("FETCH_ERROR", "Failed to load specialist: " + e.getMessage());
        }
    }

    private Map<String, Object> toMap(Specialist specialist) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", specialist.getId());
        map.put("name", specialist.getName());
        map.put("bio", specialist.getBio());
        map.put("price", specialist.getPrice());
        map.put("status", specialist.getStatus());

        try {
            if (specialist.getExpertiseList() != null) {
                List<String> expertiseIds = specialist.getExpertiseList().stream()
                        .map(e -> e.getId())
                        .collect(Collectors.toList());
                List<String> expertiseNames = specialist.getExpertiseList().stream()
                        .map(e -> e.getName())
                        .collect(Collectors.toList());
                map.put("expertiseIds", expertiseIds);
                map.put("expertiseNames", expertiseNames);
            }
        } catch (Exception e) {
            map.put("expertiseIds", List.of());
            map.put("expertiseNames", List.of());
        }

        return map;
    }
}

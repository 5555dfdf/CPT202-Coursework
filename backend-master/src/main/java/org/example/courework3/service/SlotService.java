package org.example.courework3.service;

import lombok.RequiredArgsConstructor;
import org.example.courework3.entity.Slot;
import org.example.courework3.entity.Specialist;
import org.example.courework3.repository.SlotRepository;
import org.example.courework3.repository.SpecialistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final SlotRepository slotRepository;
    private final SpecialistRepository specialistRepository;

    public List<Slot> listSlots(Map<String, Object> params) {
        String specialistId = (String) params.get("specialistId");
        String dateStr = (String) params.get("date");
        String fromStr = (String) params.get("from");
        String toStr = (String) params.get("to");
        String availableStr = (String) params.get("available");

        List<Slot> slots;

        if (specialistId != null && !specialistId.isBlank()) {
            slots = slotRepository.findBySpecialistIdOrderByStartTimeAsc(specialistId);
        } else {
            slots = slotRepository.findAll();
        }

        if (dateStr != null && !dateStr.isBlank()) {
            LocalDate date = parseDate(dateStr);
            if (date != null) {
                slots = slots.stream()
                        .filter(s -> s.getStartTime().toLocalDate().isEqual(date))
                        .toList();
            }
        }

        if (fromStr != null && !fromStr.isBlank()) {
            LocalTime fromTime = parseTime(fromStr);
            if (fromTime != null) {
                slots = slots.stream()
                        .filter(s -> !s.getStartTime().toLocalTime().isBefore(fromTime))
                        .toList();
            }
        }

        if (toStr != null && !toStr.isBlank()) {
            LocalTime toTime = parseTime(toStr);
            if (toTime != null) {
                slots = slots.stream()
                        .filter(s -> !s.getEndTime().toLocalTime().isAfter(toTime))
                        .toList();
            }
        }

        if (availableStr != null && !availableStr.isBlank()) {
            boolean available = Boolean.parseBoolean(availableStr);
            slots = slots.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getAvailable()) == available)
                    .toList();
        }

        return slots;
    }

    @Transactional
    public Slot createSlot(Map<String, Object> payload) {
        String specialistId = (String) payload.get("specialistId");
        String dateStr = (String) payload.get("date");
        String startStr = (String) payload.get("start");
        String endStr = (String) payload.get("end");
        Boolean available = payload.get("available") != null
                ? Boolean.valueOf(payload.get("available").toString())
                : true;

        if (specialistId == null || specialistId.isBlank()) {
            throw new RuntimeException("Specialist ID cannot be empty");
        }
        if (!specialistRepository.existsById(specialistId)) {
            throw new RuntimeException("Specialist not found: " + specialistId);
        }

        LocalDate date = parseDate(dateStr);
        if (date == null) {
            throw new RuntimeException("Invalid date format: " + dateStr);
        }

        LocalTime start = parseTime(startStr);
        LocalTime end = parseTime(endStr);
        if (start == null || end == null) {
            throw new RuntimeException("Invalid time format");
        }
        if (!end.isAfter(start)) {
            throw new RuntimeException("End time must be after start time");
        }

        Slot slot = new Slot();
        slot.setSpecialistId(specialistId);
        slot.setStartTime(LocalDateTime.of(date, start));
        slot.setEndTime(LocalDateTime.of(date, end));
        slot.setAvailable(available);

        return slotRepository.save(slot);
    }

    @Transactional
    public Slot updateSlot(String id, Map<String, Object> payload) {
        Slot slot = slotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + id));

        if (payload.containsKey("date")) {
            String dateStr = (String) payload.get("date");
            LocalDate date = parseDate(dateStr);
            if (date == null) {
                throw new RuntimeException("Invalid date format: " + dateStr);
            }
            LocalTime currentStart = slot.getStartTime().toLocalTime();
            LocalTime currentEnd = slot.getEndTime().toLocalTime();
            slot.setStartTime(LocalDateTime.of(date, currentStart));
            slot.setEndTime(LocalDateTime.of(date, currentEnd));
        }

        if (payload.containsKey("start")) {
            String startStr = (String) payload.get("start");
            LocalTime start = parseTime(startStr);
            if (start == null) {
                throw new RuntimeException("Invalid start time format");
            }
            LocalDate currentDate = slot.getStartTime().toLocalDate();
            slot.setStartTime(LocalDateTime.of(currentDate, start));
        }

        if (payload.containsKey("end")) {
            String endStr = (String) payload.get("end");
            LocalTime end = parseTime(endStr);
            if (end == null) {
                throw new RuntimeException("Invalid end time format");
            }
            LocalDate currentDate = slot.getEndTime().toLocalDate();
            slot.setEndTime(LocalDateTime.of(currentDate, end));
        }

        if (slot.getStartTime() != null && slot.getEndTime() != null
                && !slot.getEndTime().isAfter(slot.getStartTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        if (payload.containsKey("available")) {
            slot.setAvailable(Boolean.valueOf(payload.get("available").toString()));
        }

        return slotRepository.save(slot);
    }

    @Transactional
    public void deleteSlot(String id) {
        if (!slotRepository.existsById(id)) {
            throw new RuntimeException("Slot not found: " + id);
        }
        slotRepository.deleteById(id);
    }

    private LocalDate parseDate(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            return LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    private LocalTime parseTime(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            return LocalTime.parse(str, DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (DateTimeParseException e) {
            try {
                return LocalTime.parse(str, DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException e2) {
                try {
                    return LocalTime.parse(str, DateTimeFormatter.ofPattern("HH:mm:ss"));
                } catch (DateTimeParseException e3) {
                    return null;
                }
            }
        }
    }
}

package org.example.courework3.service;

import lombok.RequiredArgsConstructor;
import org.example.courework3.entity.Pricing;
import org.example.courework3.entity.Specialist;
import org.example.courework3.repository.PricingRepository;
import org.example.courework3.repository.SpecialistRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRepository pricingRepository;
    private final SpecialistRepository specialistRepository;

    public Object getQuote(Map<String, Object> payload) {
        String specialistId = (String) payload.get("specialistId");
        Object durationObj = payload.get("duration");
        String type = payload.containsKey("type") ? (String) payload.get("type") : null;

        Integer duration = null;
        if (durationObj != null) {
            if (durationObj instanceof Number) {
                duration = ((Number) durationObj).intValue();
            } else {
                try {
                    duration = Integer.parseInt(durationObj.toString());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (specialistId == null || specialistId.isBlank()) {
            throw new RuntimeException("Specialist ID cannot be empty");
        }

        Specialist specialist = specialistRepository.findById(specialistId).orElse(null);
        String specialistName = specialist != null ? specialist.getName() : "";
        BigDecimal defaultPrice = specialist != null ? specialist.getPrice() : null;

        boolean hasDuration = duration != null;
        boolean hasType = type != null && !type.isBlank();

        if (hasDuration && hasType) {
            // Exact match: return single result
            Optional<Pricing> found = pricingRepository
                    .findBySpecialistIdAndDurationAndType(specialistId, duration, type)
                    .stream().findFirst();
            if (found.isPresent()) {
                return toResultMap(found.get(), specialistName);
            }
            // Fall back to specialist's default price
            return Map.of(
                    "specialistId", specialistId,
                    "specialistName", specialistName,
                    "duration", duration,
                    "type", type,
                    "amount", defaultPrice != null ? defaultPrice : BigDecimal.ZERO,
                    "currency", "CNY",
                    "note", "Estimated (no specific pricing found)"
            );
        }

        // Browse mode: return list
        List<Pricing> records;
        if (hasDuration) {
            records = pricingRepository.findBySpecialistIdAndDuration(specialistId, duration);
        } else if (hasType) {
            records = pricingRepository.findBySpecialistIdAndType(specialistId, type);
        } else {
            records = pricingRepository.findBySpecialistId(specialistId);
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (Pricing p : records) {
            results.add(toResultMap(p, specialistName));
        }

        // If no records found, provide default entries for common combinations
        if (results.isEmpty()) {
            List<Integer> durations = hasDuration ? List.of(duration) : List.of(30, 45, 60, 90);
            List<String> types = hasType ? List.of(type) : List.of("online", "offline");
            for (Integer d : durations) {
                for (String t : types) {
                    results.add(Map.of(
                            "specialistId", specialistId,
                            "specialistName", specialistName,
                            "duration", d,
                            "type", t,
                            "amount", defaultPrice != null ? defaultPrice : BigDecimal.ZERO,
                            "currency", "CNY",
                            "note", "Estimated (no specific pricing found)"
                    ));
                }
            }
        }

        return results;
    }

    private Map<String, Object> toResultMap(Pricing p, String specialistName) {
        return Map.of(
                "specialistId", p.getSpecialistId() != null ? p.getSpecialistId() : "",
                "specialistName", specialistName,
                "duration", p.getDuration() != null ? p.getDuration() : 0,
                "type", p.getType() != null ? p.getType() : "",
                "amount", p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO,
                "currency", p.getCurrency() != null ? p.getCurrency() : "CNY",
                "detail", p.getDetail() != null ? p.getDetail() : ""
        );
    }
}

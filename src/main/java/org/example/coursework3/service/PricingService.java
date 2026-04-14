package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.dto.request.PricingQuoteRequest;
import org.example.coursework3.entity.Pricing;
import org.example.coursework3.repository.PricingRepository;
import org.example.coursework3.dto.response.PricingQuoteResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRepository pricingRepository;
    public List<PricingQuoteResult> getQuote(PricingQuoteRequest request) {
        String specialistId = request.getSpecialistId();
        Integer duration = request.getDuration();
        String type = request.getType();

        if (specialistId == null || specialistId.isBlank()) {
            throw new MsgException("specialistId is required");
        }

        if (type != null && type.isBlank()) {
            type = null;
        }

        List<Pricing> pricing;
        if (duration != null && type != null) {
            try {
                pricing = pricingRepository.findBySpecialistIdAndDurationAndType(specialistId, duration, type);
            } catch (Exception e) {
                throw new MsgException("Pricing not found");
            }

        } else if (duration != null) {
            try {
                pricing = pricingRepository.findFirstBySpecialistIdAndDurationOrderByCreatedAtDesc(specialistId, duration);
            } catch (Exception e) {
                throw new MsgException("Pricing not found");
            }

        } else if (type != null) {
            try {
                pricing = pricingRepository.findFirstBySpecialistIdAndTypeOrderByCreatedAtDesc(specialistId, type);
            } catch (Exception e) {
                throw new MsgException("Pricing not found");
            }
        } else {
            try {
                pricing = pricingRepository.findFirstBySpecialistIdOrderByCreatedAtDesc(specialistId);
            } catch (Exception e) {
                throw new MsgException("Pricing not found");
            }
        }

        List<PricingQuoteResult> results = new ArrayList<>();
        for (Pricing prices : pricing){
            results.add(PricingQuoteResult.changeToResult(prices));
        }

        return results;
    }

}

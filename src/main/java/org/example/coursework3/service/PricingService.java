package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.dto.request.PricingRuleRequest;
import org.example.coursework3.dto.request.PricingQuoteRequest;
import org.example.coursework3.entity.Pricing;
import org.example.coursework3.repository.SpecialistsRepository;
import org.example.coursework3.repository.PricingRepository;
import org.example.coursework3.dto.response.PricingQuoteResult;
import org.example.coursework3.vo.PricingRuleVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRepository pricingRepository;
    private final SpecialistsRepository specialistsRepository;

    public List<PricingQuoteResult> getQuote(PricingQuoteRequest request) {
        String specialistId = request.getSpecialistId();
        Integer duration = request.getDuration();
        String type = normalizeTypeForQuery(request.getType());

        if (specialistId == null || specialistId.isBlank()) {
            throw new MsgException("specialistId is required");
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

    public List<PricingRuleVo> listRules(String specialistId, Integer duration, String type) {
        String normalizedSpecialistId = normalizeSpecialistIdForQuery(specialistId);
        String normalizedType = normalizeTypeForQuery(type);

        List<Pricing> pricing = normalizedSpecialistId == null
                ? pricingRepository.findAllByOrderByUpdatedAtDescCreatedAtDesc()
                : pricingRepository.findBySpecialistIdOrderByUpdatedAtDescCreatedAtDesc(normalizedSpecialistId);

        return pricing.stream()
                .filter(rule -> duration == null || duration.equals(rule.getDuration()))
                .filter(rule -> normalizedType == null || normalizedType.equals(normalizeTypeForQuery(rule.getType())))
                .map(PricingRuleVo::fromEntity)
                .collect(Collectors.toList());
    }

    public PricingRuleVo getRule(String id) {
        Pricing pricing = pricingRepository.findById(id)
                .orElseThrow(() -> new MsgException("pricing rule not found"));
        return PricingRuleVo.fromEntity(pricing);
    }

    @Transactional
    public PricingRuleVo createRule(PricingRuleRequest request) {
        if (request == null) {
            throw new MsgException("request body is required");
        }

        String specialistId = normalizeSpecialistIdRequired(request.getSpecialistId());
        Integer duration = validateDuration(request.getDuration());
        String type = normalizeTypeRequired(request.getType());
        BigDecimal amount = normalizeAmount(request.getAmount());
        String currency = normalizeCurrency(request.getCurrency());
        String detail = normalizeDetail(request.getDetail());

        ensureSpecialistExists(specialistId);
        ensureUniqueRule(specialistId, duration, type, null);

        Pricing pricing = new Pricing();
        pricing.setSpecialistId(specialistId);
        pricing.setDuration(duration);
        pricing.setType(type);
        pricing.setAmount(amount.doubleValue());
        pricing.setCurrency(currency);
        pricing.setDetail(detail);
        pricingRepository.save(pricing);
        return PricingRuleVo.fromEntity(pricing);
    }

    @Transactional
    public PricingRuleVo updateRule(String id, PricingRuleRequest request) {
        if (request == null) {
            throw new MsgException("request body is required");
        }

        Pricing pricing = pricingRepository.findById(id)
                .orElseThrow(() -> new MsgException("pricing rule not found"));

        String specialistId = normalizeSpecialistIdRequired(request.getSpecialistId());
        Integer duration = validateDuration(request.getDuration());
        String type = normalizeTypeRequired(request.getType());
        BigDecimal amount = normalizeAmount(request.getAmount());
        String currency = normalizeCurrency(request.getCurrency());
        String detail = normalizeDetail(request.getDetail());

        ensureSpecialistExists(specialistId);
        ensureUniqueRule(specialistId, duration, type, id);

        pricing.setSpecialistId(specialistId);
        pricing.setDuration(duration);
        pricing.setType(type);
        pricing.setAmount(amount.doubleValue());
        pricing.setCurrency(currency);
        pricing.setDetail(detail);
        pricingRepository.save(pricing);
        return PricingRuleVo.fromEntity(pricing);
    }

    @Transactional
    public void deleteRule(String id) {
        if (id == null || id.isBlank()) {
            throw new MsgException("pricingRuleId is required");
        }
        if (!pricingRepository.existsById(id)) {
            throw new MsgException("pricing rule not found");
        }
        pricingRepository.deleteById(id.trim());
    }

    private void ensureSpecialistExists(String specialistId) {
        if (!specialistsRepository.existsById(specialistId)) {
            throw new MsgException("specialist not found");
        }
    }

    private void ensureUniqueRule(String specialistId, Integer duration, String type, String selfId) {
        boolean exists = selfId == null
                ? pricingRepository.existsBySpecialistIdAndDurationAndType(specialistId, duration, type)
                : pricingRepository.existsBySpecialistIdAndDurationAndTypeAndIdNot(specialistId, duration, type, selfId);
        if (exists) {
            throw new MsgException("pricing rule already exists for this specialist, duration, and type");
        }
    }

    private String normalizeSpecialistIdRequired(String specialistId) {
        String value = normalizeSpecialistIdForQuery(specialistId);
        if (value == null) {
            throw new MsgException("specialistId is required");
        }
        return value;
    }

    private String normalizeSpecialistIdForQuery(String specialistId) {
        if (specialistId == null) return null;
        String value = specialistId.trim();
        return value.isEmpty() ? null : value;
    }

    private Integer validateDuration(Integer duration) {
        if (duration == null) {
            throw new MsgException("duration is required");
        }
        if (duration <= 0) {
            throw new MsgException("duration must be greater than 0");
        }
        return duration;
    }

    private String normalizeTypeRequired(String type) {
        String value = normalizeTypeForQuery(type);
        if (value == null) {
            throw new MsgException("type is required");
        }
        if (value.length() > 20) {
            throw new MsgException("type length must not exceed 20");
        }
        return value;
    }

    private String normalizeTypeForQuery(String type) {
        if (type == null) return null;
        String value = type.trim().toLowerCase();
        return value.isEmpty() ? null : value;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new MsgException("amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new MsgException("amount must not be less than 0");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCurrency(String currency) {
        String value = currency == null ? "CNY" : currency.trim().toUpperCase();
        if (value.isEmpty()) {
            value = "CNY";
        }
        if (value.length() > 10) {
            throw new MsgException("currency length must not exceed 10");
        }
        return value;
    }

    private String normalizeDetail(String detail) {
        if (detail == null) return null;
        String value = detail.trim();
        return value.isEmpty() ? null : value;
    }

}

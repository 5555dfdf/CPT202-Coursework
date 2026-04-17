package org.example.coursework3.vo;

import lombok.Data;
import org.example.coursework3.entity.Pricing;

import java.time.LocalDateTime;

@Data
public class PricingRuleVo {
    private String id;
    private String specialistId;
    private Integer duration;
    private String type;
    private Double amount;
    private String currency;
    private String detail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PricingRuleVo fromEntity(Pricing pricing) {
        if (pricing == null) return null;
        PricingRuleVo vo = new PricingRuleVo();
        vo.setId(pricing.getId());
        vo.setSpecialistId(pricing.getSpecialistId());
        vo.setDuration(pricing.getDuration());
        vo.setType(pricing.getType());
        vo.setAmount(pricing.getAmount());
        vo.setCurrency(pricing.getCurrency());
        vo.setDetail(pricing.getDetail());
        vo.setCreatedAt(pricing.getCreatedAt());
        vo.setUpdatedAt(pricing.getUpdatedAt());
        return vo;
    }
}


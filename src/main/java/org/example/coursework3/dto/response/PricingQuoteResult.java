package org.example.coursework3.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.coursework3.entity.Pricing;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricingQuoteResult {
    private double amount;
    private String currency;
    private String detail;

    public static PricingQuoteResult changeToResult(Pricing pricing){
        return new PricingQuoteResult(pricing.getAmount(), pricing.getCurrency(), pricing.getDetail());

    }

}

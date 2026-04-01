package org.example.coursework3.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.coursework3.dto.request.PricingQuoteRequest;
import org.example.coursework3.dto.response.PricingQuoteResult;
import org.example.coursework3.result.Result;
import org.example.coursework3.service.PricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@Slf4j
public class PricingController {
    @Autowired
    private PricingService pricingService;

    @PostMapping("/pricing/quote")
    public Result<List<PricingQuoteResult>> getPriceInfo(@RequestHeader("Authorization") String authHeader, @RequestBody PricingQuoteRequest pricingQuoteRequest){
        return Result.success(pricingService.getQuote(pricingQuoteRequest));
    }

}

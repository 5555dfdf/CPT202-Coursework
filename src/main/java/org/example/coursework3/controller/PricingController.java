package org.example.coursework3.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.coursework3.dto.request.PricingRuleRequest;
import org.example.coursework3.dto.request.PricingQuoteRequest;
import org.example.coursework3.dto.response.PricingQuoteResult;
import org.example.coursework3.result.Result;
import org.example.coursework3.service.PricingService;
import org.example.coursework3.service.AuthService;
import org.example.coursework3.vo.PricingRuleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@Slf4j
public class PricingController {
    @Autowired
    private PricingService pricingService;
    @Autowired
    private AuthService authService;

    @PostMapping("/pricing/quote")
    public Result<List<PricingQuoteResult>> getPriceInfo(@RequestHeader("Authorization") String authHeader, @RequestBody PricingQuoteRequest pricingQuoteRequest){
        return Result.success(pricingService.getQuote(pricingQuoteRequest));
    }

    @GetMapping("/admin/pricing-rules")
    public Result<List<PricingRuleVo>> listPricingRules(@RequestHeader("Authorization") String authHeader,
                                                        @RequestParam(required = false) String specialistId,
                                                        @RequestParam(required = false) Integer duration,
                                                        @RequestParam(required = false) String type) {
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "please use admin role");
        }
        return Result.success(pricingService.listRules(specialistId, duration, type));
    }

    @GetMapping("/admin/pricing-rules/{id}")
    public Result<PricingRuleVo> getPricingRule(@RequestHeader("Authorization") String authHeader,
                                                @PathVariable String id) {
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "please use admin role");
        }
        return Result.success(pricingService.getRule(id));
    }

    @PostMapping("/admin/pricing-rules")
    public Result<PricingRuleVo> createPricingRule(@RequestHeader("Authorization") String authHeader,
                                                   @RequestBody PricingRuleRequest request) {
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "please use admin role");
        }
        return Result.success(pricingService.createRule(request));
    }

    @PatchMapping("/admin/pricing-rules/{id}")
    public Result<PricingRuleVo> updatePricingRule(@RequestHeader("Authorization") String authHeader,
                                                   @PathVariable String id,
                                                   @RequestBody PricingRuleRequest request) {
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "please use admin role");
        }
        return Result.success(pricingService.updateRule(id, request));
    }

    @DeleteMapping("/admin/pricing-rules/{id}")
    public Result<Void> deletePricingRule(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable String id) {
        if (!authService.verifyAsAdmin(authHeader)) {
            return Result.error("ERROR", "please use admin role");
        }
        pricingService.deleteRule(id);
        return Result.success("pricing rule deleted successfully");
    }

}

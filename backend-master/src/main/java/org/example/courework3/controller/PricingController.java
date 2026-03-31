package org.example.courework3.controller;

import lombok.RequiredArgsConstructor;
import org.example.courework3.result.Result;
import org.example.courework3.service.PricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pricing")
@CrossOrigin
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @PostMapping("/quote")
    public ResponseEntity<?> quote(@RequestBody Map<String, Object> payload) {
        try {
            Object result = pricingService.getQuote(payload);
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.error("QUOTE_ERROR", e.getMessage()));
        }
    }
}

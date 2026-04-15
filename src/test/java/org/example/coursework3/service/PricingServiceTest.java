package org.example.coursework3.service;

import org.example.coursework3.dto.request.PricingQuoteRequest;
import org.example.coursework3.dto.response.PricingQuoteResult;
import org.example.coursework3.entity.Pricing;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.PricingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PricingRepository pricingRepository;

    @InjectMocks
    private PricingService pricingService;

   @Test
    void getQuote_returnsP1ForU3_60_online() {
        Pricing p1 = pricingRow("p1", "u3", 60, "online", 100.00, "USD", "1 hour online session");
        when(pricingRepository.findBySpecialistIdAndDurationAndType("u3", 60, "online")).thenReturn(List.of(p1));

        List<PricingQuoteResult> out = pricingService.getQuote(new PricingQuoteRequest("u3", 60, "online"));

        assertEquals(1, out.size());
        assertEquals(100.00, out.get(0).getAmount());
        assertEquals("USD", out.get(0).getCurrency());
        assertEquals("1 hour online session", out.get(0).getDetail());
    }

    
    @Test
    void getQuote_returnsP2ForU3_30_online() {
        Pricing p2 = pricingRow("p2", "u3", 30, "online", 60.00, "USD", "30 min quick session");
        when(pricingRepository.findBySpecialistIdAndDurationAndType("u3", 30, "online")).thenReturn(List.of(p2));

        List<PricingQuoteResult> out = pricingService.getQuote(new PricingQuoteRequest("u3", 30, "online"));

        assertEquals(60.00, out.get(0).getAmount());
        assertEquals("30 min quick session", out.get(0).getDetail());
    }

    
    @Test
    void getQuote_returnsP3ForU4_60_offline() {
        Pricing p3 = pricingRow("p3", "u4", 60, "offline", 80.00, "USD", "Face-to-face consultation");
        when(pricingRepository.findBySpecialistIdAndDurationAndType("u4", 60, "offline")).thenReturn(List.of(p3));

        List<PricingQuoteResult> out = pricingService.getQuote(new PricingQuoteRequest("u4", 60, "offline"));

        assertEquals(80.00, out.get(0).getAmount());
        assertEquals("Face-to-face consultation", out.get(0).getDetail());
    }

    @Test
    void getQuote_throwsWhenSpecialistIdBlank() {
        MsgException ex = assertThrows(MsgException.class,
                () -> pricingService.getQuote(new PricingQuoteRequest("", 60, "online")));
        assertEquals("specialistId is required", ex.getMessage());
    }

    @Test
    void getQuote_throwsWhenRepositoryThrows() {
        when(pricingRepository.findBySpecialistIdAndDurationAndType("u3", 60, "online"))
                .thenThrow(new RuntimeException("db"));

        MsgException ex = assertThrows(MsgException.class,
                () -> pricingService.getQuote(new PricingQuoteRequest("u3", 60, "online")));
        assertEquals("Pricing not found", ex.getMessage());
    }

    private static Pricing pricingRow(String id, String specialistId, int duration, String type,
                                      double amount, String currency, String detail) {
        Pricing p = new Pricing();
        p.setId(id);
        p.setSpecialistId(specialistId);
        p.setDuration(duration);
        p.setType(type);
        p.setAmount(amount);
        p.setCurrency(currency);
        p.setDetail(detail);
        return p;
    }
}

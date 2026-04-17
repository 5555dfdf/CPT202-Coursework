package org.example.coursework3.service;

import org.example.coursework3.dto.request.PricingQuoteRequest;
import org.example.coursework3.dto.request.PricingRuleRequest;
import org.example.coursework3.dto.response.PricingQuoteResult;
import org.example.coursework3.entity.Pricing;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.PricingRepository;
import org.example.coursework3.repository.SpecialistsRepository;
import org.example.coursework3.vo.PricingRuleVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PricingRepository pricingRepository;
    @Mock
    private SpecialistsRepository specialistsRepository;

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

    @Test
    void createRule_savesNormalizedPricingRule() {
        PricingRuleRequest request = new PricingRuleRequest();
        request.setSpecialistId("u3");
        request.setDuration(60);
        request.setType("ONLINE");
        request.setAmount(new BigDecimal("128"));
        request.setCurrency("usd");
        request.setDetail("  Premium session  ");

        when(specialistsRepository.existsById("u3")).thenReturn(true);
        when(pricingRepository.existsBySpecialistIdAndDurationAndType("u3", 60, "online")).thenReturn(false);
        doAnswer(invocation -> {
            Pricing saved = invocation.getArgument(0);
            saved.setId("rule-1");
            return saved;
        }).when(pricingRepository).save(any(Pricing.class));

        PricingRuleVo out = pricingService.createRule(request);

        assertEquals("rule-1", out.getId());
        assertEquals("u3", out.getSpecialistId());
        assertEquals(60, out.getDuration());
        assertEquals("online", out.getType());
        assertEquals(128.0, out.getAmount());
        assertEquals("USD", out.getCurrency());
        assertEquals("Premium session", out.getDetail());
    }

    @Test
    void createRule_throwsWhenDuplicateExists() {
        PricingRuleRequest request = new PricingRuleRequest();
        request.setSpecialistId("u3");
        request.setDuration(60);
        request.setType("online");
        request.setAmount(new BigDecimal("100"));

        when(specialistsRepository.existsById("u3")).thenReturn(true);
        when(pricingRepository.existsBySpecialistIdAndDurationAndType("u3", 60, "online")).thenReturn(true);

        MsgException ex = assertThrows(MsgException.class, () -> pricingService.createRule(request));
        assertEquals("pricing rule already exists for this specialist, duration, and type", ex.getMessage());
    }

    @Test
    void updateRule_updatesAllEditableFields() {
        Pricing existing = pricingRow("rule-1", "u3", 60, "online", 100.00, "USD", "old");
        PricingRuleRequest request = new PricingRuleRequest();
        request.setSpecialistId("u4");
        request.setDuration(90);
        request.setType("offline");
        request.setAmount(new BigDecimal("188.50"));
        request.setCurrency("cny");
        request.setDetail("new detail");

        when(pricingRepository.findById("rule-1")).thenReturn(java.util.Optional.of(existing));
        when(specialistsRepository.existsById("u4")).thenReturn(true);
        when(pricingRepository.existsBySpecialistIdAndDurationAndTypeAndIdNot("u4", 90, "offline", "rule-1")).thenReturn(false);
        when(pricingRepository.save(any(Pricing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PricingRuleVo out = pricingService.updateRule("rule-1", request);

        assertEquals("u4", out.getSpecialistId());
        assertEquals(90, out.getDuration());
        assertEquals("offline", out.getType());
        assertEquals(188.5, out.getAmount());
        assertEquals("CNY", out.getCurrency());
        assertEquals("new detail", out.getDetail());
    }

    @Test
    void deleteRule_throwsWhenIdMissing() {
        MsgException ex = assertThrows(MsgException.class, () -> pricingService.deleteRule(" "));
        assertEquals("pricingRuleId is required", ex.getMessage());
    }

    @Test
    void listRules_filtersByDurationAndType() {
        Pricing p1 = pricingRow("r1", "u3", 60, "online", 100.00, "USD", "one");
        Pricing p2 = pricingRow("r2", "u3", 90, "offline", 180.00, "USD", null);
        when(pricingRepository.findBySpecialistIdOrderByUpdatedAtDescCreatedAtDesc("u3")).thenReturn(List.of(p1, p2));

        List<PricingRuleVo> out = pricingService.listRules("u3", 90, "OFFLINE");

        assertEquals(1, out.size());
        assertEquals("r2", out.get(0).getId());
        assertNull(out.get(0).getDetail());
        verify(pricingRepository).findBySpecialistIdOrderByUpdatedAtDescCreatedAtDesc(eq("u3"));
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

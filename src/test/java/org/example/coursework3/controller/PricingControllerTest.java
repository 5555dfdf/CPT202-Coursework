package org.example.coursework3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.coursework3.dto.request.PricingQuoteRequest;
import org.example.coursework3.dto.response.PricingQuoteResult;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.service.PricingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(PricingController.class)
@AutoConfigureMockMvc(addFilters = false)
class PricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PricingService pricingService;

    // SQL p1：('p1','u3',60,'online',100.00,'USD','1 hour online session') 
    @Test
    void quoteDrSmith60MinOnline_matchesSqlRowP1() throws Exception {
        PricingQuoteRequest request = new PricingQuoteRequest("u3", 60, "online");
        List<PricingQuoteResult> quoteResults = List.of(
                new PricingQuoteResult(100.00, "USD", "1 hour online session"));
        when(pricingService.getQuote(any(PricingQuoteRequest.class))).thenReturn(quoteResults);

        mockMvc.perform(post("/pricing/quote")
                        .header("Authorization", "Bearer token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].amount").value(100.00))
                .andExpect(jsonPath("$.data[0].currency").value("USD"))
                .andExpect(jsonPath("$.data[0].detail").value("1 hour online session"));

        verify(pricingService).getQuote(any(PricingQuoteRequest.class));
    }

    // SQL p2：('p2','u3',30,'online',60.00,'USD','30 min quick session') 
    @Test
    void quoteDrSmith30MinOnline_matchesSqlRowP2() throws Exception {
        PricingQuoteRequest request = new PricingQuoteRequest("u3", 30, "online");
        List<PricingQuoteResult> quoteResults = List.of(
                new PricingQuoteResult(60.00, "USD", "30 min quick session"));
        when(pricingService.getQuote(any(PricingQuoteRequest.class))).thenReturn(quoteResults);

        mockMvc.perform(post("/pricing/quote")
                        .header("Authorization", "Bearer token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].amount").value(60.00))
                .andExpect(jsonPath("$.data[0].currency").value("USD"))
                .andExpect(jsonPath("$.data[0].detail").value("30 min quick session"));
    }

    // SQL p3：('p3','u4',60,'offline',80.00,'USD','Face-to-face consultation') 
    @Test
    void quoteDrLee60MinOffline_matchesSqlRowP3() throws Exception {
        PricingQuoteRequest request = new PricingQuoteRequest("u4", 60, "offline");
        when(pricingService.getQuote(any(PricingQuoteRequest.class))).thenReturn(List.of(
                new PricingQuoteResult(80.00, "USD", "Face-to-face consultation")));

        mockMvc.perform(post("/pricing/quote")
                        .header("Authorization", "Bearer token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].amount").value(80.00))
                .andExpect(jsonPath("$.data[0].detail").value("Face-to-face consultation"));

        verify(pricingService).getQuote(eq(request));
    }

    @Test
    void getPriceInfoShouldReturnUnauthorizedWhenServiceThrowsMsgException() throws Exception {
        PricingQuoteRequest request = new PricingQuoteRequest("", 60, "online");
        when(pricingService.getQuote(any(PricingQuoteRequest.class)))
                .thenThrow(new MsgException("specialistId is required"));

        mockMvc.perform(post("/pricing/quote")
                        .header("Authorization", "Bearer token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("specialistId is required"));
    }
}

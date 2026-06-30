package com.api.llm.controller;

import com.api.llm.BaseIntegrationTest;
import com.api.llm.dto.BriefingLLMResponseDto;
import com.api.llm.entity.Briefing;
import com.api.llm.entity.BriefingId;
import com.api.llm.repository.BriefingRepository;
import com.api.llm.service.OllamaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BriefingsControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private BriefingRepository briefingRepository;

    @MockitoBean
    private OllamaService ollamaService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        briefingRepository.deleteAll();
        when(ollamaService.checkStatus()).thenReturn(true);
    }

    @Test
    void getById_returns200_withCachedBriefing_withoutCallingOllama() throws Exception {
        briefingRepository.save(new Briefing(
                new BriefingId("EONET_123", "DEFAULT"),
                "summary text", "moderate", "impact text",
                Instant.now(), List.of("stay indoors", "avoid windows")
        ));

        mockMvc.perform(get("/api/briefings/EONET_123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("EONET_123", "DEFAULT", 30.0, "EARTHQUAKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("EONET_123"))
                .andExpect(jsonPath("$.summary").value("summary text"))
                .andExpect(jsonPath("$.severity").value("moderate"));

        verify(ollamaService, never()).generate(anyString());
    }

    @Test
    void getById_returns200_andCallsOllama_whenNotCached() throws Exception {
        var llmResponse = buildValidLLMResponse();
        when(ollamaService.generate(anyString())).thenReturn(Mono.just(llmResponse));

        mockMvc.perform(get("/api/briefings/EONET_456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("EONET_456", "DEFAULT", 30.0, "EARTHQUAKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("EONET_456"))
                .andExpect(jsonPath("$.summary").value(llmResponse.getSummary()))
                .andExpect(jsonPath("$.impact").value(llmResponse.getImpact()));
    }

    private String requestBody(String eventId, String readingLevel, double magnitudeLevel, String category) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "eventId", eventId,
                "readingLevel", readingLevel,
                "magnitudeLevel", magnitudeLevel,
                "category", category
        ));
    }

    private BriefingLLMResponseDto buildValidLLMResponse() {
        var response = new BriefingLLMResponseDto();
        response.setSummary("A significant seismic event has been detected.");
        response.setImpact("Structural damage to buildings is possible.");
        response.setPrecautions(List.of("Drop, cover, and hold on", "Stay away from windows"));
        return response;
    }
}

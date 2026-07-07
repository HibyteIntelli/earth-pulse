package com.api.llm.controller;

import com.api.llm.BaseIntegrationTest;
import com.api.llm.dto.BriefingRequestDto;
import com.api.llm.dto.BriefingResponseDto;
import com.api.llm.service.BriefingService;
import com.api.llm.service.OllamaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthorizationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Value("${llm.internal-secret}")
    private String internalSecret;

    @MockitoBean
    private BriefingService briefingService;

    @MockitoBean
    private OllamaService ollamaService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        when(briefingService.getBriefing(any(BriefingRequestDto.class))).thenReturn(
                BriefingResponseDto.builder()
                        .eventId("EONET_123")
                        .readingLevel("DEFAULT")
                        .summary("summary text")
                        .impact("impact text")
                        .severity("moderate")
                        .precautions(List.of("stay indoors"))
                        .generatedAt(Instant.now())
                        .build());
        when(ollamaService.checkStatus()).thenReturn(true);
    }

    @Test
    void publicBriefing_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/briefings/EONET_123")
                        .param("readingLevel", "DEFAULT")
                        .param("magnitudeLevel", "30.0")
                        .param("category", "EARTHQUAKE"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicBriefing_returns200_withValidJwt() throws Exception {
        mockMvc.perform(get("/api/briefings/EONET_123")
                        .with(jwt())
                        .param("readingLevel", "DEFAULT")
                        .param("magnitudeLevel", "30.0")
                        .param("category", "EARTHQUAKE"))
                .andExpect(status().isOk());
    }

    @Test
    void internalBriefing_returns401_withoutSecretHeader() throws Exception {
        mockMvc.perform(get("/api/internal/briefings/EONET_123")
                        .param("readingLevel", "DEFAULT")
                        .param("magnitudeLevel", "30.0")
                        .param("category", "EARTHQUAKE"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalBriefing_returns401_withWrongSecret() throws Exception {
        mockMvc.perform(get("/api/internal/briefings/EONET_123")
                        .header("X-Internal-Secret", "wrong-secret")
                        .param("readingLevel", "DEFAULT")
                        .param("magnitudeLevel", "30.0")
                        .param("category", "EARTHQUAKE"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalBriefing_returns200_withCorrectSecret() throws Exception {
        mockMvc.perform(get("/api/internal/briefings/EONET_123")
                        .header("X-Internal-Secret", internalSecret)
                        .param("readingLevel", "DEFAULT")
                        .param("magnitudeLevel", "30.0")
                        .param("category", "EARTHQUAKE"))
                .andExpect(status().isOk());
    }

    @Test
    void health_isReachable_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }
}
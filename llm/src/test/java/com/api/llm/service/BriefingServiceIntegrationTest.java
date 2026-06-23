package com.api.llm.service;

import com.api.llm.dto.BriefingLLMRequestDto;
import com.api.llm.dto.BriefingLLMResponseDto;
import com.api.llm.dto.BriefingRequestDto;
import com.api.llm.dto.BriefingResponseDto;
import com.api.llm.entity.Briefing;
import com.api.llm.repository.BriefingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Slf4j
class BriefingServiceIntegrationTest {

    private BriefingService briefingService;
    private BriefingRepository briefingRepository;

    @Value("${ollama.baseUrl}")
    private String ollamaBaseUrl;

    @BeforeEach
    void setUp() {

        WebClient webClient = WebClient.builder()
                .baseUrl(ollamaBaseUrl)
                .build();

        OllamaService ollamaService = new OllamaService(webClient);
        ReflectionTestUtils.setField(ollamaService, "model", "llama3");

        briefingRepository = mock(BriefingRepository.class);
        briefingService = new BriefingService(briefingRepository, ollamaService);
    }

    @Test
    @DisplayName("getLLMSection() returns a real LLM-generated briefing")
    void getLLMSection_returnsRealLLMResponse() {
        var request = new BriefingLLMRequestDto();
        request.setCategory("earthquake");
        request.setMagnitudeLevel(0.0);
        request.setReadingLevel("SIMPLIFIED");

        BriefingLLMResponseDto result = briefingService.getLLMSection(request);

        log.info("=== Real LLM Response ===");
        log.info("Summary:     {}", result.getSummary());
        log.info("Impact:      {}", result.getImpact());
        log.info("Precautions: {}", result.getPrecautions());

        assertThat(result.getSummary()).isNotBlank();
        assertThat(result.getImpact()).isNotBlank();
        assertThat(result.getPrecautions()).isNotEmpty();
    }

    @Test
    @DisplayName("add() persists briefing then getBriefing() returns real LLM-generated response WHEN DEFAULT")
    void add_thenGetBriefing_showsRealLLMResponse() {
        when(briefingRepository.existsById(any())).thenReturn(false);

        var request = new BriefingRequestDto();
        request.setEventId("event-001");
        request.setReadingLevel("DEFAULT");
        request.setCategory("earthquake");
        request.setMagnitudeLevel(5.5);

        BriefingResponseDto result = briefingService.add(request);

        ArgumentCaptor<Briefing> captor = ArgumentCaptor.forClass(Briefing.class);
        verify(briefingRepository).save(captor.capture());
        Briefing saved = captor.getValue();

        log.info("=== Saved Briefing Entity ===");
        log.info("EventId:      {}", saved.getId().getEventId());
        log.info("ReadingLevel: {}", saved.getId().getReadingLevel());

        log.info("=== Real LLM Briefing Response ===");
        log.info("EventId:      {}", result.getEventId());
        log.info("ReadingLevel: {}", result.getReadingLevel());
        log.info("Summary:      {}", result.getSummary());
        log.info("Severity:     {}", result.getSeverity());
        log.info("Impact:       {}", result.getImpact());
        log.info("Precautions:  {}", result.getPrecautions());
        log.info("GeneratedAt:  {}", result.getGeneratedAt());

        assertThat(result.getEventId()).isEqualTo("event-001");
        assertThat(result.getReadingLevel()).isEqualTo("DEFAULT");
        assertThat(result.getSummary()).isNotBlank();
        assertThat(result.getImpact()).isNotBlank();
        assertThat(result.getPrecautions()).isNotEmpty();
        assertThat(result.getGeneratedAt()).isNotNull();
    }

    @Test
    @DisplayName("add() persists briefing then getBriefing() returns real LLM-generated response WHEN SIMPLIFIED")
    void add_thenGetBriefing_showsRealLLMResponse_SIMPLIFIED() {
        when(briefingRepository.existsById(any())).thenReturn(false);

        var request = new BriefingRequestDto();
        request.setEventId("event-001");
        request.setReadingLevel("SIMPLIFIED");
        request.setCategory("earthquake");
        request.setMagnitudeLevel(5.5);

        BriefingResponseDto result = briefingService.add(request);

        ArgumentCaptor<Briefing> captor = ArgumentCaptor.forClass(Briefing.class);
        verify(briefingRepository).save(captor.capture());
        Briefing saved = captor.getValue();


        log.info("=== Saved Briefing Entity ===");
        log.info("EventId:      {}", saved.getId().getEventId());
        log.info("ReadingLevel: {}", saved.getId().getReadingLevel());

        log.info("=== Real LLM Briefing Response ===");
        log.info("EventId:      {}", result.getEventId());
        log.info("ReadingLevel: {}", result.getReadingLevel());
        log.info("Summary:      {}", result.getSummary());
        log.info("Severity:     {}", result.getSeverity());
        log.info("Impact:       {}", result.getImpact());
        log.info("Precautions:  {}", result.getPrecautions());
        log.info("GeneratedAt:  {}", result.getGeneratedAt());

        assertThat(result.getEventId()).isEqualTo("event-001");
        assertThat(result.getReadingLevel()).isEqualTo("SIMPLIFIED");
        assertThat(result.getSummary()).isNotBlank();
        assertThat(result.getImpact()).isNotBlank();
        assertThat(result.getPrecautions()).isNotEmpty();
        assertThat(result.getGeneratedAt()).isNotNull();
    }
}

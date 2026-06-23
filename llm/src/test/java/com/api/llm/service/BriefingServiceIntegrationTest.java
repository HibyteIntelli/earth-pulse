package com.api.llm.service;

import com.api.llm.dto.BriefingLLMRequestDto;
import com.api.llm.dto.BriefingLLMResponseDto;
import com.api.llm.dto.BriefingRequestDto;
import com.api.llm.dto.BriefingResponseDto;
import com.api.llm.entity.Briefing;
import com.api.llm.repository.BriefingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BriefingServiceIntegrationTest {

    private BriefingService briefingService;
    private BriefingRepository briefingRepository;

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:11434")
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

        System.out.println("=== Real LLM Response ===");
        System.out.println("Summary:     " + result.getSummary());
        System.out.println("Impact:      " + result.getImpact());
        System.out.println("Precautions: " + result.getPrecautions());

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

        System.out.println("=== Saved Briefing Entity ===");
        System.out.println("EventId:      " + saved.getId().getEventId());
        System.out.println("ReadingLevel: " + saved.getId().getReadingLevel());

        System.out.println("\n=== Real LLM Briefing Response ===");
        System.out.println("EventId:      " + result.getEventId());
        System.out.println("ReadingLevel: " + result.getReadingLevel());
        System.out.println("Summary:      " + result.getSummary());
        System.out.println("Severity:     " + result.getSeverity());
        System.out.println("Impact:       " + result.getImpact());
        System.out.println("Precautions:  " + result.getPrecautions());
        System.out.println("GeneratedAt:  " + result.getGeneratedAt());

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

        System.out.println("=== Saved Briefing Entity ===");
        System.out.println("EventId:      " + saved.getId().getEventId());
        System.out.println("ReadingLevel: " + saved.getId().getReadingLevel());

        System.out.println("\n=== Real LLM Briefing Response ===");
        System.out.println("EventId:      " + result.getEventId());
        System.out.println("ReadingLevel: " + result.getReadingLevel());
        System.out.println("Summary:      " + result.getSummary());
        System.out.println("Severity:     " + result.getSeverity());
        System.out.println("Impact:       " + result.getImpact());
        System.out.println("Precautions:  " + result.getPrecautions());
        System.out.println("GeneratedAt:  " + result.getGeneratedAt());

        assertThat(result.getEventId()).isEqualTo("event-001");
        assertThat(result.getReadingLevel()).isEqualTo("SIMPLIFIED");
        assertThat(result.getSummary()).isNotBlank();
        assertThat(result.getImpact()).isNotBlank();
        assertThat(result.getPrecautions()).isNotEmpty();
        assertThat(result.getGeneratedAt()).isNotNull();
    }
}

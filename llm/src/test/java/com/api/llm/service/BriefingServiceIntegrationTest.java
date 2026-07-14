package com.api.llm.service;

import com.api.llm.BaseIntegrationTest;
import com.api.llm.dto.BriefingLLMResponseDto;
import com.api.llm.dto.BriefingRequestDto;
import com.api.llm.entity.Briefing;
import com.api.llm.entity.BriefingId;
import com.api.llm.repository.BriefingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BriefingServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BriefingService briefingService;

    @Autowired
    private BriefingRepository briefingRepository;

    @MockitoBean
    private OllamaService ollamaService;

    @BeforeEach
    void setUp() {
        briefingRepository.deleteAll();
        doNothing().when(ollamaService).checkStatus();
    }

    @Test
    void isCached_returnsFalse_whenBriefingNotInDatabase() {
        var request = new BriefingRequestDto("EONET_1", "DEFAULT", 5.0, "EARTHQUAKE");

        assertThat(briefingService.isCached(request)).isFalse();
    }

    @Test
    void isCached_returnsTrue_whenMatchingBriefingExists() {
        // isCached looks up by BriefingId(eventId, category)
        briefingRepository.save(new Briefing(
                new BriefingId("EONET_1", "DEFAULT"),
                "summary", "low", "impact",
                Instant.now(), List.of("precaution 1", "precaution 2")
        ));

        var request = new BriefingRequestDto("EONET_1", "DEFAULT", 5.0, "EARTHQUAKE");

        assertThat(briefingService.isCached(request)).isTrue();
    }

    @Test
    void getBriefing_callsOllamaAndSavesToDatabase_whenNotCached() {
        var llmResponse = buildValidLLMResponse();
        when(ollamaService.generate(anyString())).thenReturn(Mono.just(llmResponse));

        var request = new BriefingRequestDto("EONET_2", "DEFAULT", 30.0, "FLOOD");
        var result = briefingService.getBriefing(request);

        assertThat(result.getEventId()).isEqualTo("EONET_2");
        assertThat(result.getSummary()).isEqualTo(llmResponse.getSummary());
        assertThat(result.getImpact()).isEqualTo(llmResponse.getImpact());
        assertThat(result.getPrecautions()).containsExactlyElementsOf(llmResponse.getPrecautions());
        assertThat(briefingRepository.count()).isEqualTo(1);
        verify(ollamaService, times(1)).generate(anyString());
    }

    @Test
    void getBriefing_returnsPersistedBriefing_whenAlreadyCached() {
        // For the cache-hit path to work end-to-end:
        // isCached uses BriefingId(eventId, category) and getBriefing loads by BriefingId(eventId, readingLevel).
        // Using the same value for both category and readingLevel ensures the same DB row is found.
        var id = new BriefingId("EONET_3", "DEFAULT");
        briefingRepository.save(new Briefing(
                id, "cached summary", "high", "cached impact",
                Instant.now(), List.of("evacuate immediately", "stay low")
        ));

        var request = new BriefingRequestDto("EONET_3", "DEFAULT", 60.0, "WILDFIRE");
        var result = briefingService.getBriefing(request);

        assertThat(result.getSummary()).isEqualTo("cached summary");
        assertThat(result.getImpact()).isEqualTo("cached impact");
        assertThat(result.getSeverity()).isEqualTo("high");
        verify(ollamaService, never()).generate(anyString());
    }

    @Test
    void getBriefing_setSeverityUnknown_whenMagnitudeIsZero() {
        when(ollamaService.generate(anyString())).thenReturn(Mono.just(buildValidLLMResponse()));

        var result = briefingService.getBriefing(new BriefingRequestDto("EONET_4", "DEFAULT", 0.0, "EARTHQUAKE"));

        assertThat(result.getSeverity()).isEqualTo("unknown");
    }

    @Test
    void getBriefing_setSeverityLow_whenMagnitudeIsBelow25() {
        when(ollamaService.generate(anyString())).thenReturn(Mono.just(buildValidLLMResponse()));

        var result = briefingService.getBriefing(new BriefingRequestDto("EONET_5", "DEFAULT", 10.0, "EARTHQUAKE"));

        assertThat(result.getSeverity()).isEqualTo("low");
    }

    @Test
    void getBriefing_setSeverityModerate_whenMagnitudeIsBetween25And50() {
        when(ollamaService.generate(anyString())).thenReturn(Mono.just(buildValidLLMResponse()));

        var result = briefingService.getBriefing(new BriefingRequestDto("EONET_6", "DEFAULT", 25.0, "FLOOD"));

        assertThat(result.getSeverity()).isEqualTo("moderate");
    }

    @Test
    void getBriefing_setSeverityHigh_whenMagnitudeIsAtLeast50() {
        when(ollamaService.generate(anyString())).thenReturn(Mono.just(buildValidLLMResponse()));

        var result = briefingService.getBriefing(new BriefingRequestDto("EONET_7", "DEFAULT", 50.0, "WILDFIRE"));

        assertThat(result.getSeverity()).isEqualTo("high");
    }

    @Test
    void getBriefing_throwsRuntimeException_afterAllAttemptsReturnInvalidResponse() {
        var invalidResponse = new BriefingLLMResponseDto();
        when(ollamaService.generate(anyString())).thenReturn(Mono.just(invalidResponse));

        var request = new BriefingRequestDto("EONET_8", "DEFAULT", 20.0, "FLOOD");

        assertThatThrownBy(() -> briefingService.getBriefing(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not generate a valid response after 3 attempts");

        verify(ollamaService, times(3)).generate(anyString());
    }

    @Test
    void getBriefing_retriesAndSucceeds_whenFirstResponseIsInvalidButSecondIsValid() {
        var invalidResponse = new BriefingLLMResponseDto();
        var validResponse = buildValidLLMResponse();
        when(ollamaService.generate(anyString()))
                .thenReturn(Mono.just(invalidResponse))
                .thenReturn(Mono.just(validResponse));

        var result = briefingService.getBriefing(new BriefingRequestDto("EONET_9", "DEFAULT", 10.0, "EARTHQUAKE"));

        assertThat(result.getSummary()).isEqualTo(validResponse.getSummary());
        verify(ollamaService, times(2)).generate(anyString());
    }

    @Test
    void cleanData_removesAllBriefings() {
        briefingRepository.save(new Briefing(
                new BriefingId("EONET_10", "DEFAULT"),
                "summary", "low", "impact",
                Instant.now(), List.of("precaution 1", "precaution 2")
        ));

        briefingService.cleanData();

        assertThat(briefingRepository.count()).isZero();
    }

    private BriefingLLMResponseDto buildValidLLMResponse() {
        var response = new BriefingLLMResponseDto();
        response.setSummary("A significant seismic event has been detected in the region.");
        response.setImpact("Structural damage to buildings and infrastructure is possible.");
        response.setPrecautions(List.of("Drop, cover, and hold on", "Stay away from windows"));
        return response;
    }
}

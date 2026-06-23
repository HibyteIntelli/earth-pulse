package com.api.llm.service;

import com.api.llm.dto.BriefingLLMRequestDto;
import com.api.llm.dto.BriefingLLMResponseDto;
import com.api.llm.dto.BriefingRequestDto;
import com.api.llm.entity.Briefing;
import com.api.llm.entity.BriefingId;
import com.api.llm.repository.BriefingRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class BriefingServiceTest {

    @Mock
    private BriefingRepository briefingRepository;

    @Mock
    private OllamaService ollamaService;

    @InjectMocks
    private BriefingService briefingService;

    private BriefingRequestDto request;

    @BeforeEach
    void setUp() {
        request = new BriefingRequestDto();
        request.setEventId("event-123");
        request.setReadingLevel("SIMPLIFIED");
    }

    @Test
    @DisplayName("add() saves Briefing with correct composite ID")
    void add_savesCorrectBriefingEntity() {
        var llmResponse = new BriefingLLMResponseDto();
        llmResponse.setSummary("summary");
        llmResponse.setImpact("impact");
        llmResponse.setPrecautions(List.of("precaution"));

        when(briefingRepository.existsById(org.mockito.Mockito.any())).thenReturn(false);
        when(ollamaService.generate(anyString())).thenReturn(Mono.just(llmResponse));

        briefingService.add(request);

        ArgumentCaptor<Briefing> captor = ArgumentCaptor.forClass(Briefing.class);
        verify(briefingRepository).save(captor.capture());

        Briefing saved = captor.getValue();

        BriefingId expectedId = new BriefingId();
        expectedId.setEventId("event-123");
        expectedId.setReadingLevel("SIMPLIFIED");

        assertThat(saved.getId()).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("add() calls repository save exactly once")
    void add_callsRepositorySaveExactlyOnce() {
        var llmResponse = new BriefingLLMResponseDto();
        llmResponse.setSummary("summary");
        llmResponse.setImpact("impact");
        llmResponse.setPrecautions(List.of("precaution"));

        when(briefingRepository.existsById(org.mockito.Mockito.any())).thenReturn(false);
        when(ollamaService.generate(anyString())).thenReturn(Mono.just(llmResponse));

        briefingService.add(request);

        verify(briefingRepository).save(org.mockito.Mockito.any(Briefing.class));
    }

    @Test
    @DisplayName("getLLMSection() returns summary, impact and precautions from OllamaService")
    void getLLMSection_returnsResponseFromOllamaService() {
        var llmRequest = new BriefingLLMRequestDto();
        llmRequest.setCategory("earthquake");
        llmRequest.setMagnitudeLevel(5.5);
        llmRequest.setReadingLevel("DEFAULT");

        var expected = new BriefingLLMResponseDto();
        expected.setSummary("An earthquake of moderate magnitude.");
        expected.setImpact("Structural damage is possible.");
        expected.setPrecautions(new ArrayList<>(List.of("Stay away from windows", "Drop and cover")));

        when(ollamaService.generate(anyString())).thenReturn(Mono.just(expected));

        BriefingLLMResponseDto result = briefingService.getLLMSection(llmRequest);

        assertThat(result.getSummary()).isEqualTo("An earthquake of moderate magnitude.");
        assertThat(result.getImpact()).isEqualTo("Structural damage is possible.");
        assertThat(result.getPrecautions()).containsExactly("Stay away from windows", "Drop and cover");

        log.info("Summary: {}", result.getSummary());
        log.info("Impact: {}", result.getImpact());
        log.info("Precautions: {}", result.getPrecautions());
    }

    @Test
    @DisplayName("getLLMSection() builds prompt containing category, magnitude and reading level")
    void getLLMSection_passesPromptContainingRequestFields() {
        var llmRequest = new BriefingLLMRequestDto();
        llmRequest.setCategory("flood");
        llmRequest.setMagnitudeLevel(7.1);
        llmRequest.setReadingLevel("DEFAULT");

        when(ollamaService.generate(anyString())).thenReturn(Mono.just(new BriefingLLMResponseDto()));

        briefingService.getLLMSection(llmRequest);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(ollamaService).generate(promptCaptor.capture());

        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("flood");
        assertThat(prompt).contains("7.10");
        assertThat(prompt).contains("DEFAULT");

        log.info("Generated prompt:\n{}", prompt);
    }
}

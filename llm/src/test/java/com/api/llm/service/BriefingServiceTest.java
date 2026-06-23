package com.api.llm.service;

import com.api.llm.dto.BriefingLLMRequestDto;
import com.api.llm.dto.BriefingLLMResponseDto;
import com.api.llm.dto.BriefingRequestDto;
import com.api.llm.entity.Briefing;
import com.api.llm.entity.BriefingId;
import com.api.llm.repository.BriefingRepository;
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
        when(briefingRepository.existsById(org.mockito.Mockito.any())).thenReturn(false);

        briefingService.add(request);

        ArgumentCaptor<Briefing> captor = ArgumentCaptor.forClass(Briefing.class);
        verify(briefingRepository).save(captor.capture());

        Briefing saved = captor.getValue();

        BriefingId expectedId = new BriefingId();
        expectedId.setEventId("event-123");
        expectedId.setReadingLevel("BEGINNER");

        assertThat(saved.getId()).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("add() calls repository save exactly once")
    void add_callsRepositorySaveExactlyOnce() {
        when(briefingRepository.existsById(org.mockito.Mockito.any())).thenReturn(false);

        briefingService.add(request);

        verify(briefingRepository).save(org.mockito.Mockito.any(Briefing.class));
    }

    @Test
    @DisplayName("getLLMSection() returns summary, impact and precautions from OllamaService")
    void getLLMSection_returnsResponseFromOllamaService() {
        var llmRequest = new BriefingLLMRequestDto();
        llmRequest.setCategory("earthquake");
        llmRequest.setMagnitudeLevel(5.5);
        llmRequest.setReadingLevel("BEGINNER");

        var expected = new BriefingLLMResponseDto();
        expected.setSummary("An earthquake of moderate magnitude.");
        expected.setImpact("Structural damage is possible.");
        expected.setPrecautions(new ArrayList<>(List.of("Stay away from windows", "Drop and cover")));

        when(ollamaService.generate(anyString())).thenReturn(Mono.just(expected));

        BriefingLLMResponseDto result = briefingService.getLLMSection(llmRequest);

        assertThat(result.getSummary()).isEqualTo("An earthquake of moderate magnitude.");
        assertThat(result.getImpact()).isEqualTo("Structural damage is possible.");
        assertThat(result.getPrecautions()).containsExactly("Stay away from windows", "Drop and cover");

        System.out.println("Summary: " + result.getSummary());
        System.out.println("Impact: " + result.getImpact());
        System.out.println("Precautions: " + result.getPrecautions());
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
        assertThat(prompt).contains("EXPERT");

        System.out.println("Generated prompt:\n" + prompt);
    }
}

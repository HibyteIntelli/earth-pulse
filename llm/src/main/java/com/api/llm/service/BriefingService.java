package com.api.llm.service;

import com.api.llm.dto.BriefingLLMRequestDto;
import com.api.llm.dto.BriefingLLMResponseDto;
import com.api.llm.dto.BriefingRequestDto;
import com.api.llm.dto.BriefingResponseDto;
import com.api.llm.entity.Briefing;
import com.api.llm.entity.BriefingId;
import com.api.llm.exception.InvalidEventIdException;
import com.api.llm.exception.LlmUnavailableException;
import com.api.llm.prompt.BriefingPrompt;
import com.api.llm.repository.BriefingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BriefingService {
    private final BriefingRepository briefingRepository;
    private final OllamaService ollamaService;

    private static final Pattern EONET_ID_PATTERN = Pattern.compile("^EONET_\\d+$");

    public BriefingLLMResponseDto getLLMSection(BriefingLLMRequestDto briefingRequest) {

        String prompt = BriefingPrompt.PROMPT.formatted(briefingRequest.getCategory(), briefingRequest.getMagnitudeLevel(), briefingRequest.getReadingLevel());

        return ollamaService.generate(prompt).block();
    }

    public boolean isCached(BriefingRequestDto request) {
        var id = new BriefingId(request.getEventId(), request.getReadingLevel());

        return briefingRepository.existsById(id);
    }

    private void validateResponse(BriefingLLMResponseDto response) {
        if (response.getSummary() == null || response.getSummary().isBlank()) {
            throw new IllegalArgumentException("Missing summary");
        }

        if (response.getImpact() == null || response.getImpact().isBlank()) {
            throw new IllegalArgumentException("Missing impact");
        }

        if (response.getPrecautions() == null) {
            throw new IllegalArgumentException("Missing precautions");
        }

        List<String> precautions = response.getPrecautions().stream()
                .filter(p -> p != null && !p.isBlank())
                .toList();

        if (precautions.size() < 2 || precautions.size() > 4) {
            throw new IllegalArgumentException("Precautions must contain 2–4 non-blank items");
        }

        response.setPrecautions(precautions);
    }

    private BriefingLLMResponseDto generateValidResponse(BriefingLLMRequestDto request) {

        final int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                var response = getLLMSection(request);
                validateResponse(response);
                return response;
            } catch (WebClientRequestException | WebClientResponseException e) {
                throw new LlmUnavailableException("Ollama is unreachable", e);
            } catch (Exception e) {
                log.warn("Invalid LLM response. Attempt {}/{}", attempt, maxAttempts, e);
            }
        }

        throw new RuntimeException("Could not generate a valid response after " + maxAttempts + " attempts");
    }

    public boolean validateEventId(String eventId){
        return eventId != null && EONET_ID_PATTERN.matcher(eventId).matches();
    }

    public BriefingResponseDto getBriefing(BriefingRequestDto request) {

        if (!validateEventId(request.getEventId())) {
            throw new InvalidEventIdException("Invalid event id");
        }
        var id = new BriefingId(request.getEventId(), request.getReadingLevel());

        var briefingOpt = briefingRepository.findById(id);
        if (briefingOpt.isPresent()) {
            return new BriefingResponseDto(briefingOpt.get());
        }

        ollamaService.checkStatus();

        var llmRequest = new BriefingLLMRequestDto(request.getCategory(), request.getMagnitudeLevel(), request.getReadingLevel());

        var llmResponse = generateValidResponse(llmRequest);

        String severity;
        if (request.getMagnitudeLevel() == 0) {
            severity = "unknown";
        } else if (request.getMagnitudeLevel() < 25) {
            severity = "low";
        } else if (request.getMagnitudeLevel() < 50) {
            severity = "moderate";
        } else {
            severity = "high";
        }

        var briefing = new Briefing(id, llmResponse.getSummary(), severity, llmResponse.getImpact(), Instant.now(), llmResponse.getPrecautions());

        briefingRepository.save(briefing);

        return new BriefingResponseDto(briefing);
    }

    @Transactional
    public void cleanData() {
        long count = briefingRepository.count();
        briefingRepository.deleteAll();
        log.info("Cleaned {} briefing(s) from the database", count);
    }

}

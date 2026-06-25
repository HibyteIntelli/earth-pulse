package com.api.llm.service;

import com.api.llm.dto.BriefingLLMRequestDto;
import com.api.llm.dto.BriefingLLMResponseDto;
import com.api.llm.dto.BriefingRequestDto;
import com.api.llm.dto.BriefingResponseDto;
import com.api.llm.entity.Briefing;
import com.api.llm.entity.BriefingId;
import com.api.llm.prompt.BriefingPrompt;
import com.api.llm.repository.BriefingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BriefingService {
    private final BriefingRepository briefingRepository;
    private final OllamaService ollamaService;

    public BriefingLLMResponseDto getLLMSection(BriefingLLMRequestDto briefingRequest) {

        String prompt = BriefingPrompt.PROMPT.formatted(briefingRequest.getCategory(), briefingRequest.getMagnitudeLevel(), briefingRequest.getReadingLevel());

        return ollamaService.generate(prompt).block();
    }

    public boolean isCached(BriefingRequestDto request) {
        var id = new BriefingId(request.getEventId(), request.getCategory());

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

    private BriefingLLMResponseDto generateValidResponse(
            BriefingLLMRequestDto request) {

        final int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                var response = getLLMSection(request);

                validateResponse(response);

                return response;
            } catch (Exception e) {
                System.out.println("Invalid LLM response. Attempt " + attempt + "/" + maxAttempts);
            }
        }

        throw new RuntimeException(
                "Could not generate a valid response after " + maxAttempts + " attempts");
    }

    public BriefingResponseDto getBriefing(BriefingRequestDto request) {
        var id = new BriefingId(request.getEventId(), request.getReadingLevel());

        if (isCached(request)) {
            var briefing = briefingRepository.findById(id).orElseThrow();
            return new BriefingResponseDto(briefing);
        }

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

}

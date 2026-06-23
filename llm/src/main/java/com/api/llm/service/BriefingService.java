package com.api.llm.service;

import com.api.llm.dto.BriefingLLMRequestDto;
import com.api.llm.dto.BriefingLLMResponseDto;
import com.api.llm.dto.BriefingRequestDto;
import com.api.llm.dto.BriefingResponseDto;
import com.api.llm.entity.Briefing;
import com.api.llm.entity.BriefingId;
import com.api.llm.repository.BriefingRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class BriefingService {
    private final BriefingRepository briefingRepository;
    private final OllamaService ollamaService;

    public BriefingService(BriefingRepository briefingRepository, OllamaService ollamaService) {
        this.briefingRepository = briefingRepository;
        this.ollamaService = ollamaService;
    }

    public BriefingLLMResponseDto getLLMSection(BriefingLLMRequestDto briefingRequest) {
        String prompt = """
                You are an environmental event analyst.

                Your task is to generate a structured briefing based only on the input data.

                Event data:
                - Category: %s
                - Magnitude level: %.2f
                - Reading level: %s
                
                If Reading level = SIMPLIFIED, it means use few and simple words.
                If Reading level = DEFAULT, it means use a regular complexity and amount of words.
                
                Output rules:
                - Return ONLY valid JSON.
                - Do NOT include markdown, explanations, or extra text.
                - Do NOT assume location, population, or real-world context.
                - Use only the provided data.

                JSON format:

                {
                  "summary": "2-3 sentences explaining what this type of event typically means",
                  "impact": "1-2 sentences describing general impact of this category of event",
                  "precautions": [
                    "2-4 general safety recommendations for this category of event"
                  ]
                }

                Precautions rules:
                - MUST be general (category-level only)
                - NEVER site-specific
                - NEVER assume real-time conditions
                """.formatted(briefingRequest.getCategory(), briefingRequest.getMagnitudeLevel(), briefingRequest.getReadingLevel());

        return ollamaService.generate(prompt).block();
    }

    public boolean checkIfExists(BriefingRequestDto request) {
        var id = new BriefingId();
        id.setEventId(request.getEventId());
        id.setReadingLevel(request.getReadingLevel());

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
                System.out.println(
                        "Invalid LLM response. Attempt " + attempt + "/" + maxAttempts);
            }
        }

        throw new RuntimeException(
                "Could not generate a valid response after " + maxAttempts + " attempts");
    }

    public BriefingResponseDto add(BriefingRequestDto request) {
        var id = new BriefingId();
        id.setEventId(request.getEventId());
        id.setReadingLevel(request.getReadingLevel());

        if (checkIfExists(request)) {
            var briefing = briefingRepository.findById(id).orElseThrow();
            return BriefingResponseDto.builder()
                    .eventId(briefing.getId().getEventId())
                    .readingLevel(briefing.getId().getReadingLevel())
                    .summary(briefing.getSummary())
                    .impact(briefing.getImpact())
                    .severity(briefing.getSeverity())
                    .precautions(briefing.getPrecautions())
                    .generatedAt(briefing.getGeneratedAt())
                    .build();
        }

        var llmRequest = new BriefingLLMRequestDto();
        llmRequest.setCategory(request.getCategory());
        llmRequest.setMagnitudeLevel(request.getMagnitudeLevel());
        llmRequest.setReadingLevel(request.getReadingLevel());

        var llmResponse = generateValidResponse(llmRequest);

        var briefing = new Briefing();
        briefing.setId(id);
        briefing.setSummary(llmResponse.getSummary());
        briefing.setImpact(llmResponse.getImpact());
        briefing.setPrecautions(llmResponse.getPrecautions());
        briefing.setGeneratedAt(Instant.now());

        if (request.getMagnitudeLevel() == 0) {
            briefing.setSeverity("unknown");
        }
        else if (request.getMagnitudeLevel() < 25){
            briefing.setSeverity("low");
        }
        else if (request.getMagnitudeLevel() < 50){
            briefing.setSeverity("moderate");
        }
        else {
            briefing.setSeverity("high");
        }

        briefingRepository.save(briefing);

        return BriefingResponseDto.builder()
                .eventId(request.getEventId())
                .readingLevel(request.getReadingLevel())
                .summary(llmResponse.getSummary())
                .impact(llmResponse.getImpact())
                .precautions(llmResponse.getPrecautions())
                .generatedAt(briefing.getGeneratedAt())
                .severity(briefing.getSeverity())
                .build();
    }

}

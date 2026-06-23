package com.api.llm.service;

import com.api.llm.dto.BriefingLLMResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class OllamaService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ollama.model}")
    private String model;

    public OllamaService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<BriefingLLMResponseDto> generate(String prompt) {

        var request = new OllamaRequest(model, prompt, false, "json");

        return webClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaResponse.class)
                .timeout(Duration.ofSeconds(120))
                .map(res -> {
                    try {
                        return objectMapper.readValue(
                                res.response(),
                                BriefingLLMResponseDto.class
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to parse LLM response", e);
                    }
                });
    }

    private record OllamaRequest(String model, String prompt, boolean stream, String format) {}

    private record OllamaResponse(String response) {}
}

package com.api.llm.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final WebClient webClient;

    public HealthController(WebClient.Builder webClientBuilder, @Value("${ollama.baseUrl}")  String llmUrl) {
        this.webClient = webClientBuilder
                .baseUrl(llmUrl)
                .build();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {

        try {
            webClient.get()
                    .uri("/api/version")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return ResponseEntity.ok(
                    Map.of("status", "UP")
            );

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("status", "DOWN"));
        }
    }
}
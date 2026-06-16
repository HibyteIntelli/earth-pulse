package com.api.llm.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

// To do

@RestController
@RequestMapping("/api")
@Slf4j
public class HealthController {

    private final WebClient webClient;

    public HealthController(WebClient ollamaWebClient) {
        this.webClient = ollamaWebClient;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {

        try {
            webClient.get()
                    .uri("/api/version")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(3));

            return ResponseEntity.ok(
                    Map.of("status", "UP")
            );

        } catch (Exception e) {
            log.warn("Ollama health check failed: {}", e.getMessage());
            return ResponseEntity.status(503)
                    .body(Map.of("status", "DOWN"));
        }
    }
}



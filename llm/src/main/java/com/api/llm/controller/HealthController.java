package com.api.llm.controller;

import com.api.llm.exception.LlmUnavailableException;
import com.api.llm.service.OllamaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
public class HealthController {

    private final OllamaService ollamaService;

    public HealthController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        try {
            ollamaService.checkStatus();
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("status", "UP"));
        } catch (LlmUnavailableException e) {
            log.error("Ollama health check failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "DOWN"));
        }
    }
}



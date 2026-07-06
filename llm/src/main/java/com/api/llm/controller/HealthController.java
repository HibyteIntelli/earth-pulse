package com.api.llm.controller;

import com.api.llm.service.OllamaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final OllamaService ollamaService;

    public HealthController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        if (ollamaService.checkStatus()) {
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("status", "UP"));
        }
        else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "DOWN"));
        }
    }
}



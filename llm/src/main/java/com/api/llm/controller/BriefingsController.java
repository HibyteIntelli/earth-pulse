package com.api.llm.controller;

import com.api.llm.dto.BriefingResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// TO DO

@RestController
@RequestMapping("/api/briefings")
public class BriefingsController {
    @GetMapping("/{id}")
    public ResponseEntity<BriefingResponseDto> getById(@PathVariable long id) {

        var dummy = BriefingResponseDto.builder()
                .eventId(id)
                .readingLevel("NOT IMPLEMENTED")
                .summary("NOT IMPLEMENTED")
                .impact("NOT IMPLEMENTED")
                .severity("NOT IMPLEMENTED")
                .precautions(new ArrayList<>(List.of("NOT IMPLEMENTED", "NOT IMPLEMENTED")))
                .generatedAt(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(dummy);
    }
}



package com.api.llm.controller;

import com.api.llm.dto.BriefingResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// To do

@RestController
@RequestMapping("/api/internal")
public class InternalController {

    @GetMapping("/briefings/{id}")
    public ResponseEntity<BriefingResponseDto> getById(@RequestHeader("X-Internal-Secret") String secret, @PathVariable String id) {

        if (!"my-secret".equals(secret)) {
            return ResponseEntity.status(403).build();
        }

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



package com.api.llm.controller;

import com.api.llm.dto.BriefingRequestDto;
import com.api.llm.dto.BriefingResponseDto;
import com.api.llm.service.BriefingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal")
public class InternalController {

    private final BriefingService briefingService;

    public InternalController(BriefingService briefingService) {
        this.briefingService = briefingService;
    }

    @GetMapping("/briefings/{id}")
    public ResponseEntity<BriefingResponseDto> getById(
            @PathVariable String id,
            @RequestParam String readingLevel,
            @RequestParam double magnitudeLevel,
            @RequestParam String category) {

        BriefingResponseDto response = briefingService.getBriefing(
                new BriefingRequestDto(id, readingLevel, magnitudeLevel, category));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/briefings")
    public ResponseEntity<Void> cleanData() {
        briefingService.cleanData();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}



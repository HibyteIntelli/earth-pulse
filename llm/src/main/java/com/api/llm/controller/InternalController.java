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
    public ResponseEntity<BriefingResponseDto> getById(@RequestHeader("X-Internal-Secret") String secret, @PathVariable String id, @RequestBody BriefingRequestDto request) {

        if (!"my-secret".equals(secret)) {
            return ResponseEntity.status(403).build();
        }

        BriefingResponseDto response = briefingService.getBriefing(
                new BriefingRequestDto(id, request.getReadingLevel(), request.getMagnitudeLevel(), request.getCategory()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}



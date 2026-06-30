package com.api.llm.controller;

import com.api.llm.dto.BriefingRequestDto;
import com.api.llm.dto.BriefingResponseDto;
import com.api.llm.service.BriefingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/briefings")
public class BriefingsController {

    private final BriefingService briefingService;

    public BriefingsController(BriefingService briefingService) {
        this.briefingService = briefingService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<BriefingResponseDto> getById(
            @PathVariable String id,
            @RequestBody BriefingRequestDto request) {

        BriefingResponseDto response = briefingService.getBriefing(
                new BriefingRequestDto(id, request.getReadingLevel(), request.getMagnitudeLevel(), request.getCategory()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}



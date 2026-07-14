package com.earthpulse.www.controller;

import com.earthpulse.www.dto.EventQueryDto;
import com.earthpulse.www.dto.MatchingWatchDto;
import com.earthpulse.www.dto.WatchMatchRequestDto;
import com.earthpulse.www.service.WatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/watches")
@RequiredArgsConstructor
public class InternalWatchController {

    private final WatchService watchService;

    @PostMapping("/match")
    public ResponseEntity<List<MatchingWatchDto>> findMatch(
            @Valid @RequestBody WatchMatchRequestDto request
    ) {
        return ResponseEntity.ok(watchService.findMatchingByEvent(request));
    }

    @PostMapping("/matching")
    public ResponseEntity<List<MatchingWatchDto>> findMatching(
            @Valid @RequestBody EventQueryDto query
    ) {
        return ResponseEntity.ok(watchService.findMatching(query));
    }
}

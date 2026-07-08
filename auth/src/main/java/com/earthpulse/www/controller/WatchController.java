package com.earthpulse.www.controller;

import com.earthpulse.www.dto.WatchRequestDto;
import com.earthpulse.www.dto.WatchResponseDto;
import com.earthpulse.www.dto.WatchUpdateDto;
import com.earthpulse.www.service.WatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/watches")
@RequiredArgsConstructor
public class WatchController {

    private final WatchService watchService;

    @GetMapping
    public ResponseEntity<List<WatchResponseDto>> list(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(watchService.list(principalId(userId)));
    }

    @PostMapping
    public ResponseEntity<WatchResponseDto> create(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody WatchRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(watchService.create(principalId(userId), dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WatchResponseDto> get(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(watchService.get(principalId(userId), id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WatchResponseDto> update(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id,
            @Valid @RequestBody WatchUpdateDto dto
    ) {
        return ResponseEntity.ok(watchService.update(principalId(userId), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id
    ) {
        watchService.delete(principalId(userId), id);
        return ResponseEntity.noContent().build();
    }

    private UUID principalId(String userId) {
        if (userId == null) throw new IllegalStateException("No authenticated principal");
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Authenticated principal is not a valid UUID: " + userId);
        }
    }
}

package com.earthpulse.www.controller;

import com.earthpulse.www.dto.UserProfileDto;
import com.earthpulse.www.service.AvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/account/avatar")
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarService avatarService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileDto> upload(
            @AuthenticationPrincipal String userId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(avatarService.upload(UUID.fromString(userId), file));
    }
}

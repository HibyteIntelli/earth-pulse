package com.earthpulse.www.dto;

import com.earthpulse.www.enums.ReadingLevel;

import java.time.Instant;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String email,
        String name,
        ReadingLevel readingLevel,
        String profilePictureUrl,
        Instant createdAt
) {}

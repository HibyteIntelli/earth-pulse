package com.earthpulse.www.dto;

import com.earthpulse.www.enums.DigestMode;
import com.earthpulse.www.enums.EventCategory;
import com.earthpulse.www.enums.ReadingLevel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WatchResponseDto(
        UUID id,
        String name,
        double minLat,
        double maxLat,
        double minLon,
        double maxLon,
        List<EventCategory> categories,
        DigestMode digestMode,
        ReadingLevel readingLevel,
        boolean active,
        Instant createdAt
) {}

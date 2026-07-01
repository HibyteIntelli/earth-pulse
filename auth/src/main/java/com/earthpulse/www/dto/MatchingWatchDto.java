package com.earthpulse.www.dto;

import com.earthpulse.www.enums.ReadingLevel;

import java.util.List;
import java.util.UUID;

public record MatchingWatchDto(
        UUID watchId,
        UUID userId,
        boolean digestMode,
        ReadingLevel readingLevel,
        List<String> categories
) {}

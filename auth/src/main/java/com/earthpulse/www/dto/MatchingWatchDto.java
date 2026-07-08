package com.earthpulse.www.dto;

import com.earthpulse.www.enums.DigestMode;
import com.earthpulse.www.enums.EventCategory;
import com.earthpulse.www.enums.ReadingLevel;

import java.util.List;
import java.util.UUID;

public record MatchingWatchDto(
        UUID watchId,
        UUID userId,
        DigestMode digestMode,
        ReadingLevel readingLevel,
        List<EventCategory> categories
) {}

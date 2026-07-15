package com.earthpulse.www.dto;

import com.earthpulse.www.enums.EventCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record WatchMatchRequestDto(
        String eventId,

        @NotNull(message = "categories are required")
        @NotEmpty(message = "categories must not be empty")
        List<EventCategory> categories,

        @NotNull(message = "point is required")
        @Valid
        GeoJsonPointDto point
) {}

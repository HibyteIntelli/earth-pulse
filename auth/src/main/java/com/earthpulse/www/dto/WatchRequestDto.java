package com.earthpulse.www.dto;

import com.earthpulse.www.enums.DigestMode;
import com.earthpulse.www.enums.EventCategory;
import com.earthpulse.www.enums.ReadingLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WatchRequestDto(

        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotNull(message = "minLat is required")
        @DecimalMin(value = "-90.0", message = "minLat must be >= -90")
        @DecimalMax(value = "90.0", message = "minLat must be <= 90")
        Double minLat,

        @NotNull(message = "maxLat is required")
        @DecimalMin(value = "-90.0", message = "maxLat must be >= -90")
        @DecimalMax(value = "90.0", message = "maxLat must be <= 90")
        Double maxLat,

        @NotNull(message = "minLon is required")
        @DecimalMin(value = "-180.0", message = "minLon must be >= -180")
        @DecimalMax(value = "180.0", message = "minLon must be <= 180")
        Double minLon,

        @NotNull(message = "maxLon is required")
        @DecimalMin(value = "-180.0", message = "maxLon must be >= -180")
        @DecimalMax(value = "180.0", message = "maxLon must be <= 180")
        Double maxLon,

        @NotNull(message = "categories is required — send an empty list to match all categories")
        @Valid List<@NotNull(message = "Category entry must not be null") EventCategory> categories,

        @NotNull(message = "digestMode is required")
        DigestMode digestMode,

        ReadingLevel readingLevel
) {}

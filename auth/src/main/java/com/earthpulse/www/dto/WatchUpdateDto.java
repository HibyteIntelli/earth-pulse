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

public record WatchUpdateDto(

        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @DecimalMin(value = "-90.0", message = "minLat must be >= -90")
        @DecimalMax(value = "90.0", message = "minLat must be <= 90")
        Double minLat,

        @DecimalMin(value = "-90.0", message = "maxLat must be >= -90")
        @DecimalMax(value = "90.0", message = "maxLat must be <= 90")
        Double maxLat,

        @DecimalMin(value = "-180.0", message = "minLon must be >= -180")
        @DecimalMax(value = "180.0", message = "minLon must be <= 180")
        Double minLon,

        @DecimalMin(value = "-180.0", message = "maxLon must be >= -180")
        @DecimalMax(value = "180.0", message = "maxLon must be <= 180")
        Double maxLon,

        @Valid List<@NotNull(message = "Category entry must not be null") EventCategory> categories,

        DigestMode digestMode,

        ReadingLevel readingLevel,

        Boolean active
) {}

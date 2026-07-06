package com.earthpulse.www.dto;

import com.earthpulse.www.enums.ReadingLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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

        @Valid List<@NotBlank(message = "Category must not be blank") @Size(max = 255, message = "Category must be at most 255 characters") String> categories,

        Boolean digestMode,

        ReadingLevel readingLevel,

        Boolean active
) {}

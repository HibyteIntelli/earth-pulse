package com.earthpulse.www.dto;

import com.earthpulse.www.enums.EventCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record EventQueryDto(

        @NotNull(message = "lat is required")
        @DecimalMin(value = "-90.0", message = "lat must be >= -90")
        @DecimalMax(value = "90.0", message = "lat must be <= 90")
        Double lat,

        @NotNull(message = "lon is required")
        @DecimalMin(value = "-180.0", message = "lon must be >= -180")
        @DecimalMax(value = "180.0", message = "lon must be <= 180")
        Double lon,

        @NotNull(message = "category is required")
        EventCategory category
) {}

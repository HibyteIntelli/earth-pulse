package com.earthpulse.www.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GeoJsonPointDto(
        @NotNull(message = "type is required")
        String type,

        @NotNull(message = "coordinates are required")
        @Size(min = 2, max = 2, message = "coordinates must have exactly 2 elements [lon, lat]")
        List<Double> coordinates
) {
    public double lon() { return coordinates.get(0); }
    public double lat() { return coordinates.get(1); }
}

package com.earthpulse.www.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EventCategory {
    DROUGHT("drought"),
    DUST_HAZE("dustHaze"),
    EARTHQUAKES("earthquakes"),
    FLOODS("floods"),
    LANDSLIDES("landslides"),
    MANMADE("manmade"),
    SEA_LAKE_ICE("seaLakeIce"),
    SEVERE_STORMS("severeStorms"),
    SNOW("snow"),
    TEMP_EXTREMES("tempExtremes"),
    VOLCANOES("volcanoes"),
    WATER_COLOR("waterColor"),
    WILDFIRES("wildfires");

    private final String value;

    EventCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static EventCategory fromValue(String value) {
        for (EventCategory c : values()) {
            if (c.value.equals(value)) return c;
        }
        throw new IllegalArgumentException("Unknown event category: " + value);
    }
}

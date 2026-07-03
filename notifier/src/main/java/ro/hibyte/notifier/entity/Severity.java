package ro.hibyte.notifier.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum Severity {
    LOW,
    MODERATE,
    HIGH,
    UNKNOWN;

    @JsonCreator
    public static Severity fromValue(String value) {
        if (value == null) return UNKNOWN;
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(UNKNOWN);
    }
}


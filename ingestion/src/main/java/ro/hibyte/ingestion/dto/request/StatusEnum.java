package ro.hibyte.ingestion.dto.request;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StatusEnum {
    OPEN("open"),
    CLOSED("closed"),
    ALL("all");

    private final String value;

    StatusEnum(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }
}

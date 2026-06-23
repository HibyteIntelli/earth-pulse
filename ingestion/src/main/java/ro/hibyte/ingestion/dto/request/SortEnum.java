package ro.hibyte.ingestion.dto.request;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SortEnum {
    EVENT_DATE_DESC("eventDate:desc"),
    EVENT_DATE_ASC("eventDate:asc"),
    INGESTED_AT_DESC("ingestedAt:desc"),
    INGESTED_AT_ASC("ingestedAt:asc");

    private final String value;

    SortEnum(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }
}

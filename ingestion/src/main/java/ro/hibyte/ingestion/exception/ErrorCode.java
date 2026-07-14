package ro.hibyte.ingestion.exception;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ErrorCode {
    INVALID_BBOX("invalid_bbox"),
    SIZE_OUT_OF_RANGE("size_out_of_range"),
    PAGE_OUT_OF_RANGE("page_out_of_range"),
    UNAUTHORIZED("unauthorized"),
    EONET_UNAVAILABLE("eonet_unavailable"),
    MALFORMED_REQUEST("malformed_request"),
    INTERNAL_ERROR("internal_error");

    private final String value;

    ErrorCode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}

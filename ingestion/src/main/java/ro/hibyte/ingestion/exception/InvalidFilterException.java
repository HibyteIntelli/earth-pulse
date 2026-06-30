package ro.hibyte.ingestion.exception;

import lombok.Getter;

@Getter
public class InvalidFilterException extends RuntimeException {

    private final String code;

    public InvalidFilterException(String code, String message) {
        super(message);
        this.code = code;
    }
}

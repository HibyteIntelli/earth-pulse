package ro.hibyte.ingestion.exception;

import org.springframework.http.HttpStatus;

public class InvalidFilterException extends IngestionException {

    public InvalidFilterException(ErrorCode code, String message) {
        super(code, HttpStatus.BAD_REQUEST, message);
    }
}

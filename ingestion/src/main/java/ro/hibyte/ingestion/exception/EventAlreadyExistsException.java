package ro.hibyte.ingestion.exception;

import org.springframework.http.HttpStatus;

public class EventAlreadyExistsException extends IngestionException {
    public EventAlreadyExistsException(String message) {
        super(ErrorCode.EVENT_ALREADY_EXISTS, HttpStatus.CONFLICT, message);
    }
}

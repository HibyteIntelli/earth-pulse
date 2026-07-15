package ro.hibyte.ingestion.exception;

import org.springframework.http.HttpStatus;

public class EonetUnavailableException extends IngestionException {
    public EonetUnavailableException(String message) {
        super(ErrorCode.EONET_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public EonetUnavailableException(String message, Throwable cause) {
        super(ErrorCode.EONET_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}

package ro.hibyte.ingestion.exception;

import org.springframework.http.HttpStatus;

public class EonetUnavailableException extends IngestionException {
    public EonetUnavailableException(String message) {
        super(ErrorCode.EONET_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}

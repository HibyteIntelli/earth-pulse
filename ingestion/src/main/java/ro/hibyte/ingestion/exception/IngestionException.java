package ro.hibyte.ingestion.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class IngestionException extends RuntimeException{
    ErrorCode code;
    HttpStatus status;

    protected IngestionException(ErrorCode code, HttpStatus status, String message){
        super(message);
        this.code = code;
        this.status = status;
    }
}

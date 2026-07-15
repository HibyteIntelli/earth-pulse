package ro.hibyte.ingestion.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ro.hibyte.ingestion.dto.response.ErrorResponse;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IngestionException.class)
    public ResponseEntity<ErrorResponse> handleIngestionException(IngestionException e) {
        ErrorResponse errorResponse = ErrorResponse.of(e.getCode(), e.getMessage());
        return new ResponseEntity<>(errorResponse, e.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        boolean isServerError = statusCode.is5xxServerError();
        ErrorCode code = isServerError ? ErrorCode.INTERNAL_ERROR : ErrorCode.MALFORMED_REQUEST;

        String message;
        if (isServerError) {
            log.error("Unhandled Spring MVC exception", ex);
            message = "An unexpected error occurred";
        } else {
            message = ex.getMessage();
        }

        ErrorResponse errorResponse = ErrorResponse.of(code, message);
        return new ResponseEntity<>(errorResponse, headers, statusCode);
    }
}

package ro.hibyte.ingestion.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import ro.hibyte.ingestion.dto.response.ErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsEonetUnavailableExceptionToItsOwnCodeAndStatus() {
        EonetUnavailableException ex = new EonetUnavailableException("EONET is down");

        ResponseEntity<ErrorResponse> response = handler.handleIngestionException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getCode()).isEqualTo("eonet_unavailable");
        assertThat(response.getBody().getMessage()).isEqualTo("EONET is down");
    }

    @Test
    void mapsInvalidFilterExceptionToBadRequest() {
        InvalidFilterException ex = new InvalidFilterException(ErrorCode.SIZE_OUT_OF_RANGE, "size must be between 1 and 500");

        ResponseEntity<ErrorResponse> response = handler.handleIngestionException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("size_out_of_range");
    }

    @Test
    void unexpectedExceptionsDoNotLeakTheirMessage() {
        RuntimeException ex = new RuntimeException("sensitive internal detail: password=secret123");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getCode()).isEqualTo("internal_error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void exceptionsInternalDoNotLeakTheirMessage() {
        RuntimeException ex = new RuntimeException("sensitive internal detail: class EventService faulty");
        WebRequest request = mock(WebRequest.class);

        ResponseEntity<Object> response = handler.handleExceptionInternal(
                ex, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);

        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body.getCode()).isEqualTo("internal_error");
        assertThat(body.getMessage()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void springHandledExceptionsGetTheSameErrorResponseFormat() {
        Exception ex = new Exception("Request body is missing");
        WebRequest request = mock(WebRequest.class);

        ResponseEntity<Object> response = handler.handleExceptionInternal(
                ex, null, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getCode()).isEqualTo("malformed_request");
        assertThat(body.getMessage()).isEqualTo("Request body is missing");
    }
}

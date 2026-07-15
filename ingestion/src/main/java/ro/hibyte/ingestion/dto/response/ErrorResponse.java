package ro.hibyte.ingestion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.hibyte.ingestion.exception.ErrorCode;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code.getValue(), message);
    }
}

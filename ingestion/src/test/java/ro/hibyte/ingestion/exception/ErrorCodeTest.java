package ro.hibyte.ingestion.exception;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesToItsValueNotItsEnumName() {
        String json = objectMapper.writeValueAsString(ErrorCode.INVALID_BBOX);

        assertThat(json).isEqualTo("\"invalid_bbox\"");
    }
}

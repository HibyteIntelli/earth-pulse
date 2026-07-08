package ro.hibyte.notifier.dto;

import lombok.Data;
import ro.hibyte.notifier.entity.Severity;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class BriefingResponseDto {
    private String eventId;
    private String readingLevel;
    private String summary;
    private String impact;
    private Severity severity;
    private List<String> precautions;
    private OffsetDateTime generatedAt;
}

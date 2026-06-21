package ro.hibyte.notifier.dto;

import lombok.Builder;
import lombok.Value;
import ro.hibyte.notifier.entity.Severity;

import java.util.List;

@Value
@Builder
public class BriefingSnapshotDto {
    String summary;
    String impact;
    Severity severity;
    List<String> precautions;
}

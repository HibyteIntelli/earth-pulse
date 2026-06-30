package ro.hibyte.notifier.dto;

import lombok.Data;
import ro.hibyte.notifier.entity.DeliveryMode;
import ro.hibyte.notifier.entity.ReadingLevel;

import java.util.UUID;

@Data
public class MatchedWatchDto {

    private UUID watchId;
    private UUID userId;
    private String userEmail;
    private DeliveryMode digestMode;
    private ReadingLevel readingLevel;
}

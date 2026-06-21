package ro.hibyte.notifier.dto;

import lombok.Builder;
import lombok.Value;
import ro.hibyte.notifier.entity.DeliveryMode;
import ro.hibyte.notifier.entity.NotificationLog;
import ro.hibyte.notifier.entity.ReadingLevel;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class NotificationDto {

    UUID id;
    UUID watchId;
    String eventId;
    String eventTitle;
    List<String> eventCategories;
    String eventUrl;
    OffsetDateTime eventDate;
    DeliveryMode deliveryMode;
    ReadingLevel readingLevel;
    OffsetDateTime deliveredAt;
    BriefingSnapshotDto briefing;

    public static NotificationDto from(NotificationLog log) {
        return NotificationDto.builder()
                .id(log.getId())
                .watchId(log.getWatchId())
                .eventId(log.getEventId())
                .eventTitle(log.getEventTitle())
                .eventCategories(log.getEventCategories())
                .eventUrl(log.getEventUrl())
                .eventDate(log.getEventDate())
                .deliveryMode(log.getDeliveryMode())
                .readingLevel(log.getReadingLevel())
                .deliveredAt(log.getDeliveredAt())
                .briefing(BriefingSnapshotDto.builder()
                        .summary(log.getBriefingSummary())
                        .impact(log.getBriefingImpact())
                        .severity(log.getBriefingSeverity())
                        .precautions(log.getBriefingPrecautions())
                        .build())
                .build();
    }
}

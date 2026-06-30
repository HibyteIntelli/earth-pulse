package ro.hibyte.notifier.dto;

import ro.hibyte.ingestion.dto.request.CategoryEnum;
import ro.hibyte.notifier.entity.DeliveryMode;
import ro.hibyte.notifier.entity.NotificationLog;
import ro.hibyte.notifier.entity.ReadingLevel;

import java.time.OffsetDateTime;
import java.util.List;

public class NotificationDto {

    private final java.util.UUID id;
    private final java.util.UUID watchId;
    private final String eventId;
    private final String eventTitle;
    private final List<CategoryEnum> eventCategories;
    private final String eventUrl;
    private final OffsetDateTime eventDate;
    private final DeliveryMode deliveryMode;
    private final ReadingLevel readingLevel;
    private final OffsetDateTime deliveredAt;
    private final BriefingSnapshotDto briefing;

    public NotificationDto(
            java.util.UUID id,
            java.util.UUID watchId,
            String eventId,
            String eventTitle,
            List<CategoryEnum> eventCategories,
            String eventUrl,
            OffsetDateTime eventDate,
            DeliveryMode deliveryMode,
            ReadingLevel readingLevel,
            OffsetDateTime deliveredAt,
            BriefingSnapshotDto briefing
    ) {
        this.id = id;
        this.watchId = watchId;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.eventCategories = eventCategories;
        this.eventUrl = eventUrl;
        this.eventDate = eventDate;
        this.deliveryMode = deliveryMode;
        this.readingLevel = readingLevel;
        this.deliveredAt = deliveredAt;
        this.briefing = briefing;
    }

    public static NotificationDto from(NotificationLog log) {
        return new NotificationDto(
                log.getId(),
                log.getWatchId(),
                log.getEventId(),
                log.getEventTitle(),
                log.getEventCategories()
                        .stream()
                        .map(CategoryEnum::fromValue)
                        .filter(java.util.Optional::isPresent)
                        .map(java.util.Optional::get)
                        .toList(),
                log.getEventUrl(),
                log.getEventDate(),
                log.getDeliveryMode(),
                log.getReadingLevel(),
                log.getDeliveredAt(),
                new BriefingSnapshotDto(
                        log.getBriefingSummary(),
                        log.getBriefingImpact(),
                        log.getBriefingSeverity(),
                        log.getBriefingPrecautions()
                )
        );
    }

    public java.util.UUID getId() { return id; }
    public java.util.UUID getWatchId() { return watchId; }
    public String getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public List<CategoryEnum> getEventCategories() { return eventCategories; }
    public String getEventUrl() { return eventUrl; }
    public OffsetDateTime getEventDate() { return eventDate; }
    public DeliveryMode getDeliveryMode() { return deliveryMode; }
    public ReadingLevel getReadingLevel() { return readingLevel; }
    public OffsetDateTime getDeliveredAt() { return deliveredAt; }
    public BriefingSnapshotDto getBriefing() { return briefing; }
}
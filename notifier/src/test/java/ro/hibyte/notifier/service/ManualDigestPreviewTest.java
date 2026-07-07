package ro.hibyte.notifier.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ro.hibyte.notifier.entity.DeliveryMode;
import ro.hibyte.notifier.entity.NotificationLog;
import ro.hibyte.notifier.entity.ReadingLevel;
import ro.hibyte.notifier.entity.Severity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Throwaway manual test — sends one daily-digest email to the locally running Mailpit
 * (docker-compose: localhost:8025 UI) with a few fake events of mixed severities, so you can
 * eyeball the rendered digest template (header count, severity-desc/date-desc ordering).
 * Not part of the permanent suite — delete after use.
 */
@SpringBootTest
class ManualDigestPreviewTest {

    @Autowired
    private NotificationEmailService notificationEmailService;

    @Test
    void previewDigestEmail() {
        UUID watchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        NotificationLog wildfire = notificationLog(watchId, userId, "EONET_7001",
                "Wildfire - Los Padres National Forest", now.minusHours(2), Severity.HIGH,
                "A wildfire has been detected, spreading across dry brush fueled by strong seasonal winds.",
                "Nearby communities may experience heavy smoke and reduced air quality.",
                List.of("Stay indoors and keep windows closed", "Follow evacuation orders if issued"));

        NotificationLog flood = notificationLog(watchId, userId, "EONET_7002",
                "Flooding - Danube River Basin", now.minusHours(5), Severity.MODERATE,
                "River levels have risen above flood stage following heavy rainfall upstream.",
                "Low-lying roads and fields near the riverbank may be impassable.",
                List.of("Avoid driving through flooded roads"));

        NotificationLog drought = notificationLog(watchId, userId, "EONET_7003",
                "Drought conditions - Central Plains", now.minusDays(1), Severity.LOW,
                "Extended dry conditions continue across the region with below-average rainfall.",
                "Minimal immediate impact; agricultural monitoring advised.",
                List.of());

        notificationEmailService.sendDigestEmail("preview@earth-pulse.local", List.of(wildfire, flood, drought));
    }

    private NotificationLog notificationLog(UUID watchId, UUID userId, String eventId, String title,
                                             OffsetDateTime eventDate, Severity severity,
                                             String summary, String impact, List<String> precautions) {
        return NotificationLog.builder()
                .watchId(watchId)
                .userId(userId)
                .eventId(eventId)
                .eventTitle(title)
                .eventCategories(List.of("wildfires"))
                .eventUrl("http://localhost:4200/events/" + eventId)
                .eventDate(eventDate)
                .deliveryMode(DeliveryMode.DAILY_DIGEST)
                .readingLevel(ReadingLevel.DEFAULT)
                .briefingSummary(summary)
                .briefingImpact(impact)
                .briefingSeverity(severity)
                .briefingPrecautions(precautions)
                .build();
    }
}

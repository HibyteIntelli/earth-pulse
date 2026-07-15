package ro.hibyte.notifier.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ro.hibyte.notifier.dto.BriefingSnapshotDto;
import ro.hibyte.notifier.dto.MatchedWatchDto;
import ro.hibyte.notifier.dto.NewEventPayloadDto;
import ro.hibyte.notifier.entity.CategoryEnum;
import ro.hibyte.notifier.entity.DeliveryMode;
import ro.hibyte.notifier.entity.ReadingLevel;
import ro.hibyte.notifier.entity.Severity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@SpringBootTest
class EmailPreviewTool {

    @Autowired
    private NotificationEmailService notificationEmailService;

    @Test
    void previewImmediateEmail() {
        MatchedWatchDto watch = new MatchedWatchDto();
        watch.setWatchId(UUID.randomUUID());
        watch.setUserId(UUID.randomUUID());
        watch.setUserEmail("preview@earth-pulse.local");
        watch.setDigestMode(DeliveryMode.IMMEDIATE);
        watch.setReadingLevel(ReadingLevel.DEFAULT);

        NewEventPayloadDto payload = new NewEventPayloadDto();
        payload.setEventId("EONET_6512");
        payload.setTitle("Wildfire - Los Padres National Forest");
        payload.setCategories(List.of(CategoryEnum.WILDFIRES));
        payload.setEventDate(OffsetDateTime.now());
        payload.setIngestedAt(OffsetDateTime.now());

        BriefingSnapshotDto briefing = BriefingSnapshotDto.builder()
                .summary("A wildfire has been detected in the Los Padres National Forest, spreading across dry brush " +
                        "in mountainous terrain fueled by strong seasonal winds.")
                .impact("Nearby communities may experience heavy smoke and reduced air quality. Evacuation orders " +
                        "are possible if the fire continues to spread toward populated areas.")
                .severity(Severity.HIGH)
                .precautions(List.of(
                        "Stay indoors and keep windows closed if you smell smoke",
                        "Follow local evacuation orders immediately if issued",
                        "Keep an emergency kit ready"
                ))
                .build();

        String eventUrl = "http://localhost:4200/map?event=" + payload.getEventId();

        notificationEmailService.sendImmediateEmail(watch, payload, briefing, eventUrl);
    }
}

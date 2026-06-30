package ro.hibyte.notifier.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.hibyte.ingestion.dto.request.CategoryEnum;
import ro.hibyte.notifier.dto.BriefingSnapshotDto;
import ro.hibyte.notifier.dto.NewEventPayloadDto;
import ro.hibyte.notifier.entity.DeliveryMode;
import ro.hibyte.notifier.entity.NotificationLog;
import ro.hibyte.notifier.entity.ReadingLevel;
import ro.hibyte.notifier.entity.Severity;
import ro.hibyte.notifier.repository.NotificationLogRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProcessingService {

    private final NotificationLogRepository notificationLogRepository;

    public void processNewEvent(NewEventPayloadDto payload) {
        log.info("Received new event: {}", payload.getEventId());

        // TODO: call Auth Service to get matching watches for this event's geometry + categories
        List<MatchedWatch> matchedWatches = List.of(); // placeholder

        for (MatchedWatch watch : matchedWatches) {
            if (notificationLogRepository.existsByWatchIdAndEventId(watch.watchId(), payload.getEventId())) {
                log.debug("Skipping duplicate delivery for watch={} event={}", watch.watchId(), payload.getEventId());
                continue;
            }

            // TODO: call LLM Service to fetch briefing for (eventId, watch.readingLevel())
            BriefingSnapshotDto briefing = null; // placeholder

            NotificationLog log = NotificationLog.builder()
                    .watchId(watch.watchId())
                    .userId(watch.userId())
                    .eventId(payload.getEventId())
                    .eventTitle(payload.getTitle())
                    .eventCategories(
                            payload.getCategories()
                                    .stream()
                                    .map(CategoryEnum::getValue)
                                    .toList()
                    )
                    .eventUrl(buildEventUrl(payload.getEventId()))
                    .eventDate(payload.getEventDate())
                    .deliveryMode(watch.deliveryMode())
                    .readingLevel(watch.readingLevel())
                    .briefingSummary(briefing != null ? briefing.getSummary() : "")
                    .briefingImpact(briefing != null ? briefing.getImpact() : "")
                    .briefingSeverity(briefing != null ? briefing.getSeverity() : Severity.LOW)
                    .briefingPrecautions(briefing != null ? briefing.getPrecautions() : List.of())
                    .build();

            notificationLogRepository.save(log);

            if (watch.deliveryMode() == DeliveryMode.IMMEDIATE) {
                // TODO: send immediate email — on success, stamp delivery:
                // log.setDeliveredAt(OffsetDateTime.now());
                // notificationLogRepository.save(log);
            }
            // DAILY_DIGEST: deliveredAt stays null; the digest job sets it after sending
        }
    }

    private String buildEventUrl(String eventId) {
        // TODO: replace with configured frontend base URL
        return "/events/" + eventId;
    }

    // Placeholder until Auth Service client is introduced
    private record MatchedWatch(UUID watchId, UUID userId, DeliveryMode deliveryMode, ReadingLevel readingLevel) {}
}

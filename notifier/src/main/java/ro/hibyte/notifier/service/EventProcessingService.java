package ro.hibyte.notifier.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.hibyte.notifier.client.AuthServiceClient;
import ro.hibyte.notifier.client.LlmServiceClient;
import ro.hibyte.notifier.entity.CategoryEnum;
import ro.hibyte.notifier.dto.BriefingResponseDto;
import ro.hibyte.notifier.dto.BriefingSnapshotDto;
import ro.hibyte.notifier.dto.MatchedWatchDto;
import ro.hibyte.notifier.dto.NewEventPayloadDto;
import ro.hibyte.notifier.entity.DeliveryMode;
import ro.hibyte.notifier.entity.NotificationLog;
import ro.hibyte.notifier.entity.Severity;
import ro.hibyte.notifier.repository.NotificationLogRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProcessingService {

    private final NotificationLogRepository notificationLogRepository;
    private final AuthServiceClient authServiceClient;
    private final LlmServiceClient llmServiceClient;

    public void processNewEvent(NewEventPayloadDto payload) {
        log.info("Received new event: {}", payload.getEventId());

        List<MatchedWatchDto> matchedWatches = authServiceClient.matchWatches(payload);

        for (MatchedWatchDto watch : matchedWatches) {
            if (notificationLogRepository.existsByWatchIdAndEventId(watch.getWatchId(), payload.getEventId())) {
                log.debug("Skipping duplicate delivery for watch={} event={}", watch.getWatchId(), payload.getEventId());
                continue;
            }

            BriefingSnapshotDto briefing = fetchBriefing(payload.getEventId(), watch);

            NotificationLog notificationLog = NotificationLog.builder()
                    .watchId(watch.getWatchId())
                    .userId(watch.getUserId())
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
                    .deliveryMode(watch.getDigestMode())
                    .readingLevel(watch.getReadingLevel())
                    .briefingSummary(briefing.getSummary())
                    .briefingImpact(briefing.getImpact())
                    .briefingSeverity(briefing.getSeverity())
                    .briefingPrecautions(briefing.getPrecautions())
                    .build();

            notificationLogRepository.save(notificationLog);

            if (watch.getDigestMode() == DeliveryMode.IMMEDIATE) {
                // TODO: send immediate email — on success, stamp delivery:
                // notificationLog.setDeliveredAt(OffsetDateTime.now());
                // notificationLogRepository.save(notificationLog);
            }
            // DAILY_DIGEST: deliveredAt stays null; the digest job sets it after sending
        }
    }

    private BriefingSnapshotDto fetchBriefing(String eventId, MatchedWatchDto watch) {
        try {
            BriefingResponseDto response = llmServiceClient.fetchBriefing(eventId, watch.getReadingLevel());
            if (response == null) return emptyBriefing();
            return BriefingSnapshotDto.builder()
                    .summary(response.getSummary())
                    .impact(response.getImpact())
                    .severity(response.getSeverity() != null ? response.getSeverity() : Severity.UNKNOWN)
                    .precautions(response.getPrecautions() != null ? response.getPrecautions() : List.of())
                    .build();
        } catch (Exception e) {
            log.warn("Failed to fetch briefing for event={} readingLevel={}: {}",
                    eventId, watch.getReadingLevel(), e.getMessage());
            return emptyBriefing();
        }
    }

    private BriefingSnapshotDto emptyBriefing() {
        return BriefingSnapshotDto.builder()
                .summary("")
                .impact("")
                .severity(Severity.UNKNOWN)
                .precautions(List.of())
                .build();
    }

    private String buildEventUrl(String eventId) {
        // TODO: replace with configured frontend base URL
        return "/events/" + eventId;
    }

}

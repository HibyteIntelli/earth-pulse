package ro.hibyte.notifier.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ro.hibyte.notifier.client.AuthServiceClient;
import ro.hibyte.notifier.client.LlmServiceClient;
import ro.hibyte.notifier.entity.CategoryEnum;
import ro.hibyte.notifier.dto.BriefingResponseDto;
import ro.hibyte.notifier.dto.BriefingSnapshotDto;
import ro.hibyte.notifier.dto.MatchedWatchDto;
import ro.hibyte.notifier.dto.NewEventPayloadDto;
import ro.hibyte.notifier.entity.DeliveryMode;
import ro.hibyte.notifier.entity.DigestQueue;
import ro.hibyte.notifier.entity.NotificationLog;
import ro.hibyte.notifier.entity.Severity;
import ro.hibyte.notifier.repository.DigestQueueRepository;
import ro.hibyte.notifier.repository.NotificationLogRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProcessingService {

    private final NotificationLogRepository notificationLogRepository;
    private final DigestQueueRepository digestQueueRepository;
    private final AuthServiceClient authServiceClient;
    private final LlmServiceClient llmServiceClient;
    private final NotificationEmailService notificationEmailService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public void processNewEvent(NewEventPayloadDto payload) {
        log.info("Received new event: {}", payload.getEventId());

        List<MatchedWatchDto> matchedWatches = authServiceClient.matchWatches(payload);

        for (MatchedWatchDto watch : matchedWatches) {
            if (notificationLogRepository.existsByWatchIdAndEventId(watch.getWatchId(), payload.getEventId())) {
                log.debug("Skipping duplicate delivery for watch={} event={}", watch.getWatchId(), payload.getEventId());
                continue;
            }

            BriefingSnapshotDto briefing = fetchBriefing(payload.getEventId(), watch);
            String eventUrl = buildEventUrl(payload.getEventId());

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
                    .eventUrl(eventUrl)
                    .eventDate(payload.getEventDate())
                    .deliveryMode(watch.getDigestMode())
                    .readingLevel(watch.getReadingLevel())
                    .briefingSummary(briefing.getSummary())
                    .briefingImpact(briefing.getImpact())
                    .briefingSeverity(briefing.getSeverity())
                    .briefingPrecautions(briefing.getPrecautions())
                    .build();

            try {
                notificationLogRepository.saveAndFlush(notificationLog);
            } catch (DataIntegrityViolationException e) {
                log.debug("Duplicate delivery race detected for watch={} event={}, skipping",
                        watch.getWatchId(), payload.getEventId());
                continue;
            }

            if (watch.getDigestMode() == DeliveryMode.IMMEDIATE) {
                try {
                    notificationEmailService.sendImmediateEmail(watch, payload, briefing, eventUrl);
                    notificationLog.setDeliveredAt(OffsetDateTime.now());
                    notificationLogRepository.save(notificationLog);
                } catch (Exception e) {
                    log.error("Failed to send immediate email for watch={} event={}: {}. Releasing claim so a future retry can resend.",
                            watch.getWatchId(), payload.getEventId(), e.getMessage());
                    notificationLogRepository.delete(notificationLog);
                }
            } else {
                 try {
                    DigestQueue digestEntry = DigestQueue.builder()
                            .watchId(watch.getWatchId())
                            .eventId(payload.getEventId())
                            .userId(watch.getUserId())
                            .userEmail(watch.getUserEmail())
                            .readingLevel(watch.getReadingLevel())
                            .matchedAt(OffsetDateTime.now())
                            .build();
                    digestQueueRepository.save(digestEntry);
                } catch (Exception e) {
                    log.error("Failed to buffer digest entry for watch={} event={}: {}. Releasing claim so a future retry can requeue.",
                            watch.getWatchId(), payload.getEventId(), e.getMessage());
                    notificationLogRepository.delete(notificationLog);
                }
            }
        }
    }

    private BriefingSnapshotDto fetchBriefing(String eventId, MatchedWatchDto watch) {
        try {
            BriefingResponseDto response = llmServiceClient.fetchBriefing(eventId, watch.getReadingLevel());
            if (response == null) return emptyBriefing();
            return BriefingSnapshotDto.builder()
                    .summary(response.getSummary() != null ? response.getSummary() : "")
                    .impact(response.getImpact() != null ? response.getImpact() : "")
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
        String encodedEventId = URLEncoder.encode(eventId, StandardCharsets.UTF_8);
        return frontendBaseUrl + "/map?event=" + encodedEventId;
    }

}

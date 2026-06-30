package ro.hibyte.notifier.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ro.hibyte.notifier.client.IngestionServiceClient;
import ro.hibyte.notifier.dto.IngestionEventPageDto;
import ro.hibyte.notifier.dto.IngestionEventResponseDto;
import ro.hibyte.notifier.dto.NewEventPayloadDto;
import ro.hibyte.notifier.entity.CategoryEnum;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Slf4j
public class EventPollingService {

    private final IngestionServiceClient ingestionServiceClient;
    private final EventProcessingService eventProcessingService;

    @Value("${notifier.polling.page-size:100}")
    private int pageSize;

    private volatile OffsetDateTime lastPolledAt = OffsetDateTime.now();

    public EventPollingService(IngestionServiceClient ingestionServiceClient,
                               EventProcessingService eventProcessingService) {
        this.ingestionServiceClient = ingestionServiceClient;
        this.eventProcessingService = eventProcessingService;
    }

    @Scheduled(fixedDelayString = "${notifier.polling.interval-ms:60000}")
    public void pollNewEvents() {
        log.debug("Polling ingestion for events since {}", lastPolledAt);
        int page = 0;
        int processed = 0;
        OffsetDateTime maxIngestedAt = null;

        try {
            IngestionEventPageDto result;
            do {
                result = ingestionServiceClient.fetchEventsSince(lastPolledAt, page, pageSize);
                if (result == null || result.getItems() == null || result.getItems().isEmpty()) break;

                for (IngestionEventResponseDto event : result.getItems()) {
                    eventProcessingService.processNewEvent(toPayload(event));
                    processed++;
                    if (event.getIngestedAt() != null &&
                            (maxIngestedAt == null || event.getIngestedAt().isAfter(maxIngestedAt))) {
                        maxIngestedAt = event.getIngestedAt();
                    }
                }
                page++;
            } while (result.getItems().size() == pageSize);

            if (maxIngestedAt != null) {
                lastPolledAt = maxIngestedAt;
            }
            log.info("Poll complete: {} new event(s) processed", processed);
        } catch (Exception e) {
            // lastPolledAt is not advanced — next poll retries from the same window
            log.warn("Polling failed, will retry next interval: {}", e.getMessage());
        }
    }

    private NewEventPayloadDto toPayload(IngestionEventResponseDto event) {
        NewEventPayloadDto dto = new NewEventPayloadDto();
        dto.setEventId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setCategories(
                event.getCategory().stream()
                        .map(CategoryEnum::fromValue)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList()
        );
        dto.setGeometry(event.getGeometry());
        dto.setMagnitudeValue(event.getMagnitudeValue());
        dto.setMagnitudeUnit(event.getMagnitudeUnit());
        dto.setEventDate(event.getEventDate());
        dto.setIngestedAt(event.getIngestedAt());
        return dto;
    }
}

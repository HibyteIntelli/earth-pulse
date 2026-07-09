package ro.hibyte.ingestion.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import ro.hibyte.ingestion.dto.notifier.NewEventPayloadDto;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotifierClient {
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final int BACKOFF_MULTIPLIER = 2;

    private final RestClient notifierRestClient;

    @Async
    public void notifyNewEvent(NewEventPayloadDto payload) {
        long backoffMs = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                notifierRestClient.post().uri("/internal/events/new").body(payload).retrieve().toBodilessEntity();
                return;
            } catch (HttpClientErrorException e) {
                log.error("Notifier returned {} for event {}", e.getStatusCode(), payload.getEventId(), e);
                return;
            } catch (HttpServerErrorException | ResourceAccessException e) {
                if (attempt == MAX_ATTEMPTS) {
                    logGivingUp(payload, attempt, e);
                    return;
                }
                log.debug("Attempt {}/{} to notify Notifier of new event {} failed, retrying in {}ms",
                        attempt, MAX_ATTEMPTS, payload.getEventId(), backoffMs);
                if (!sleep(backoffMs)) {
                    return;
                }
                backoffMs *= BACKOFF_MULTIPLIER;
            } catch (Exception e) {
                logGivingUp(payload, attempt, e);
                return;
            }
        }
    }

    private void logGivingUp(NewEventPayloadDto payload, int attempt, Throwable e) {
        log.error("Failed to notify Notifier of new event {} after {} attempt(s)", payload.getEventId(), attempt, e);
    }

    private boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry wait for event notification interrupted, aborting further attempts", e);
            return false;
        }
    }
}

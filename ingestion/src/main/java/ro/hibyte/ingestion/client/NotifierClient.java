package ro.hibyte.ingestion.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import ro.hibyte.ingestion.dto.notifier.NewEventPayloadDto;

@Component
@Slf4j
public class NotifierClient {

    private final int maxAttempts;
    private final long initialBackoffMs;
    private final int backoffMultiplier;

    private final RestClient notifierRestClient;

    public NotifierClient(
            RestClient notifierRestClient,
            @Value("${notifier.retry.max-attempts:3}") int maxAttempts,
            @Value("${notifier.retry.initial-backoff-ms:1000}") long initialBackoffMs,
            @Value("${notifier.retry.backoff-multiplier:2}") int backoffMultiplier
    ) {
        this.notifierRestClient = notifierRestClient;
        this.maxAttempts = maxAttempts;
        this.initialBackoffMs = initialBackoffMs;
        this.backoffMultiplier = backoffMultiplier;
    }

    @Async
    public void notifyNewEvent(NewEventPayloadDto payload) {
        long backoffMs = initialBackoffMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                notifierRestClient.post().uri("/internal/events/new").body(payload).retrieve().toBodilessEntity();
                return;
            } catch (HttpClientErrorException e) {
                log.error("Notifier returned {} for event {}", e.getStatusCode(), payload.getEventId(), e);
                return;
            } catch (HttpServerErrorException | ResourceAccessException e) {
                if (attempt == maxAttempts) {
                    logGivingUp(payload, attempt, e);
                    return;
                }
                log.debug("Attempt {}/{} to notify Notifier of new event {} failed, retrying in {}ms",
                        attempt, maxAttempts, payload.getEventId(), backoffMs);
                if (!sleep(backoffMs)) {
                    return;
                }
                backoffMs *= backoffMultiplier;
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

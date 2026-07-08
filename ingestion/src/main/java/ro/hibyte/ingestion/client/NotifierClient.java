package ro.hibyte.ingestion.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ro.hibyte.ingestion.dto.notifier.NewEventPayloadDto;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
@Slf4j
public class NotifierClient {
    private final RestClient restClient;

    public NotifierClient(
            @Value("${app.notifier-service.url}") String notifierUrl,
            @Value("${app.notifier-service.internal-secret}") String internalSecret
    ) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(notifierUrl)
                .defaultHeader("X-Internal-Secret", internalSecret)
                .requestFactory(factory)
                .build();
    }

    @Async
    public void notifyNewEvent(NewEventPayloadDto payload) {
        try {
            restClient.post().uri("/internal/events/new").body(payload).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to notify Notifier of new event {}", payload.getEventId(), e);
        }
    }
}

package ro.hibyte.notifier.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ro.hibyte.notifier.dto.IngestionEventPageDto;

import java.time.OffsetDateTime;

@Component
public class IngestionServiceClient {

    private final RestClient restClient;

    public IngestionServiceClient(
            @Value("${app.ingestion-service.url}") String ingestionServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(ingestionServiceUrl)
                .build();
    }

    public IngestionEventPageDto fetchEventsSince(OffsetDateTime since, int page, int size) {
        var filter = new EventFilter(since, "ingestedAt:asc", size, page);

        return restClient.post()
                .uri("/events/search")
                .body(filter)
                .retrieve()
                .body(IngestionEventPageDto.class);
    }

    private record EventFilter(
            OffsetDateTime since,
            String sort,
            int size,
            int page
    ) {}
}

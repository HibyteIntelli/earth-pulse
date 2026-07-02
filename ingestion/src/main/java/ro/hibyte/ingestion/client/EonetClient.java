package ro.hibyte.ingestion.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ro.hibyte.ingestion.dto.eonet.EonetResponse;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class EonetClient {

    private final RestClient eonetRestClient;
    private final ObjectMapper objectMapper;

    public EonetResponse fetchEvents(int days) {
        String json = eonetRestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/events")
                        .queryParam("status", "all")
                        .queryParam("days", days)
                        .build())
                .retrieve()
                .body(String.class);

        return objectMapper.readValue(json, EonetResponse.class);
    }
}

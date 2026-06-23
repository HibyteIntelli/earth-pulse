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

    public EonetResponse fetchEvents() {
        String json = eonetRestClient
                .get()
                .uri("/api/v3/events?status=all&days=30")
                .retrieve()
                .body(String.class);

        return objectMapper.readValue(json, EonetResponse.class);
    }
}

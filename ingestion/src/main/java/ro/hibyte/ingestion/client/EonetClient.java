package ro.hibyte.ingestion.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ro.hibyte.ingestion.dto.eonet.EonetEvent;
import ro.hibyte.ingestion.dto.eonet.EonetResponse;
import ro.hibyte.ingestion.exception.EonetUnavailableException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EonetClient {

    private final RestClient eonetRestClient;
    private final ObjectMapper objectMapper;

    public EonetResponse fetchEvents(int days) {
        String json;
        try {
            json = eonetRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/events")
                            .queryParam("status", "all")
                            .queryParam("days", days)
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch events from EONET (days={})", days, e);
            throw new EonetUnavailableException("EONET is unavailable: " + e.getMessage());
        }

        if (json == null) {
            log.error("EONET returned an empty response body (days={})", days);
            throw new EonetUnavailableException("EONET returned an empty response body");
        }

        List<EonetEvent> events = new ArrayList<>();
        for (String eventJson : splitEvents(json)) {
            try {
                events.add(objectMapper.readValue(eventJson, EonetEvent.class));
            } catch (Exception e) {
                log.warn("Skipping malformed EONET event: {}", e.getMessage());
            }
        }

        EonetResponse response = new EonetResponse();
        response.setEvents(events);
        return response;
    }

    private List<String> splitEvents(String json) {
        List<String> result = new ArrayList<>();
        int eventsKey = json.indexOf("\"events\"");
        int arrayStart = eventsKey >= 0 ? json.indexOf('[', eventsKey) : -1;
        if (arrayStart < 0) return result;

        int depth = 0;
        int objStart = -1;
        boolean inString = false;
        boolean escape = false;

        for (int i = arrayStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escape) escape = false;
                else if (c == '\\') escape = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    result.add(json.substring(objStart, i + 1));
                    objStart = -1;
                }
            } else if (c == ']' && depth == 0) {
                break;
            }
        }
        return result;
    }
}

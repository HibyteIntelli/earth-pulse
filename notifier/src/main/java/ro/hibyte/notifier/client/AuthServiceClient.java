package ro.hibyte.notifier.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ro.hibyte.notifier.dto.EventMatchRequestDto;
import ro.hibyte.notifier.dto.EventMatchResponseDto;
import ro.hibyte.notifier.dto.MatchedWatchDto;
import ro.hibyte.notifier.dto.NewEventPayloadDto;
import ro.hibyte.notifier.entity.CategoryEnum;

import java.util.List;

@Component
public class AuthServiceClient {

    private final RestClient restClient;

    public AuthServiceClient(
            @Value("${app.auth-service.url}") String authServiceUrl,
            @Value("${app.auth-service.internal-secret}") String internalSecret) {
        this.restClient = RestClient.builder()
                .baseUrl(authServiceUrl)
                .defaultHeader("X-Internal-Secret", internalSecret)
                .build();
    }

    public List<MatchedWatchDto> matchWatches(NewEventPayloadDto payload) {
        EventMatchRequestDto request = new EventMatchRequestDto(
                payload.getEventId(),
                payload.getCategories().stream().map(CategoryEnum::getValue).toList(),
                payload.getGeometry()
        );

        EventMatchResponseDto response = restClient.post()
                .uri("/internal/watches/match")
                .body(request)
                .retrieve()
                .body(EventMatchResponseDto.class);

        return response != null && response.getMatches() != null
                ? response.getMatches()
                : List.of();
    }
}

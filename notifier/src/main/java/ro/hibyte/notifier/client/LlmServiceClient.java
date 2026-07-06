package ro.hibyte.notifier.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ro.hibyte.notifier.dto.BriefingResponseDto;
import ro.hibyte.notifier.entity.ReadingLevel;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class LlmServiceClient {

    private final RestClient restClient;

    public LlmServiceClient(
            @Value("${app.llm-service.url}") String llmServiceUrl,
            @Value("${app.llm-service.internal-secret}") String internalSecret) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(llmServiceUrl)
                .defaultHeader("X-Internal-Secret", internalSecret)
                .requestFactory(factory)
                .build();
    }

    public BriefingResponseDto fetchBriefing(String eventId, ReadingLevel readingLevel) {
        return restClient.get()
                .uri("/api/internal/briefings/{eventId}?readingLevel={readingLevel}",
                        eventId, readingLevel.name())
                .retrieve()
                .body(BriefingResponseDto.class);
    }
}

package ro.hibyte.notifier.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ro.hibyte.notifier.client.AuthServiceClient;
import ro.hibyte.notifier.client.LlmServiceClient;
import ro.hibyte.notifier.dto.BriefingResponseDto;
import ro.hibyte.notifier.dto.MatchedWatchDto;
import ro.hibyte.notifier.dto.NewEventPayloadDto;
import ro.hibyte.notifier.entity.CategoryEnum;
import ro.hibyte.notifier.entity.DeliveryMode;
import ro.hibyte.notifier.entity.NotificationLog;
import ro.hibyte.notifier.entity.ReadingLevel;
import ro.hibyte.notifier.entity.Severity;
import ro.hibyte.notifier.repository.NotificationLogRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class EventProcessingConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> mailpit = new GenericContainer<>(DockerImageName.parse("axllent/mailpit:latest"))
            .withExposedPorts(1025, 8025);

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", mailpit::getHost);
        registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
    }

    @MockitoBean
    private AuthServiceClient authServiceClient;

    @MockitoBean
    private LlmServiceClient llmServiceClient;

    @Autowired
    private EventProcessingService eventProcessingService;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Test
    void concurrentDeliveries_forSameWatchAndEvent_sendExactlyOneEmail() throws InterruptedException {
        UUID watchId = UUID.randomUUID();
        String eventId = "EONET_CONCURRENCY_" + UUID.randomUUID();
        String userEmail = "concurrency-test@earth-pulse.local";

        MatchedWatchDto watch = new MatchedWatchDto();
        watch.setWatchId(watchId);
        watch.setUserId(UUID.randomUUID());
        watch.setUserEmail(userEmail);
        watch.setDigestMode(DeliveryMode.IMMEDIATE);
        watch.setReadingLevel(ReadingLevel.DEFAULT);

        NewEventPayloadDto payload = new NewEventPayloadDto();
        payload.setEventId(eventId);
        payload.setTitle("Concurrency test wildfire");
        payload.setCategories(List.of(CategoryEnum.WILDFIRES));
        payload.setEventDate(OffsetDateTime.now());
        payload.setIngestedAt(OffsetDateTime.now());

        BriefingResponseDto briefing = new BriefingResponseDto();
        briefing.setSummary("Test summary");
        briefing.setImpact("Test impact");
        briefing.setSeverity(Severity.HIGH);
        briefing.setPrecautions(List.of("Stay alert"));

        when(authServiceClient.matchWatches(any())).thenReturn(List.of(watch));
        when(llmServiceClient.fetchBriefing(anyString(), any())).thenReturn(briefing);

        int threadCount = 2;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    eventProcessingService.processNewEvent(payload);
                });
            }

            boolean allReady = readyLatch.await(5, TimeUnit.SECONDS);
            assertThat(allReady).isTrue();
            startLatch.countDown();
            executor.shutdown();
            boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);
            assertThat(finished).isTrue();
        }

        List<NotificationLog> matchingLogs = notificationLogRepository.findAll().stream()
                .filter(n -> n.getWatchId().equals(watchId) && n.getEventId().equals(eventId))
                .toList();
        assertThat(matchingLogs).hasSize(1);
        assertThat(matchingLogs.getFirst().getDeliveredAt()).isNotNull();

        String mailpitApiUrl = "http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025);
        var response = RestClient.create()
                .get()
                .uri(mailpitApiUrl + "/api/v1/messages")
                .retrieve()
                .body(MailpitMessages.class);

        assertThat(response).isNotNull();
        long deliveredToTestAddress = response.messages().stream()
                .filter(m -> m.to().stream().anyMatch(a -> a.address().equals(userEmail)))
                .count();
        assertThat(deliveredToTestAddress).isEqualTo(1);
    }

    private record MailpitMessages(List<MailpitMessage> messages) {}
    private record MailpitMessage(@JsonProperty("To") List<MailpitAddress> to) {}
    private record MailpitAddress(@JsonProperty("Address") String address) {}
}

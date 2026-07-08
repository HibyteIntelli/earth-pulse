package ro.hibyte.notifier.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
import ro.hibyte.notifier.entity.DigestQueue;
import ro.hibyte.notifier.entity.NotificationLog;
import ro.hibyte.notifier.entity.ReadingLevel;
import ro.hibyte.notifier.entity.Severity;
import ro.hibyte.notifier.repository.DigestQueueRepository;
import ro.hibyte.notifier.repository.NotificationLogRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class DigestJobConcurrencyIntegrationTest {

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

    @MockitoSpyBean
    private NotificationEmailService notificationEmailService;

    @Autowired
    private DigestJobService digestJobService;

    @Autowired
    private EventProcessingService eventProcessingService;

    @Autowired
    private DigestQueueRepository digestQueueRepository;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Test
    void matchBufferedWhileJobIsSending_survivesTheJobsCleanup() throws Exception {
        UUID watchId = UUID.randomUUID();
        String userEmail = "digest-race@earth-pulse.local";
        String eventIdA = "EONET_DIGEST_A_" + UUID.randomUUID();
        String eventIdB = "EONET_DIGEST_B_" + UUID.randomUUID();

        MatchedWatchDto watch = new MatchedWatchDto();
        watch.setWatchId(watchId);
        watch.setUserId(UUID.randomUUID());
        watch.setUserEmail(userEmail);
        watch.setDigestMode(DeliveryMode.DAILY_DIGEST);
        watch.setReadingLevel(ReadingLevel.DEFAULT);

        when(authServiceClient.matchWatches(any())).thenReturn(List.of(watch));
        when(llmServiceClient.fetchBriefing(anyString(), any())).thenReturn(briefingResponse());

        eventProcessingService.processNewEvent(payload(eventIdA));
        assertThat(digestQueueRepository.findByWatchId(watchId)).hasSize(1);

        CountDownLatch jobReachedSendLatch = new CountDownLatch(1);
        CountDownLatch producerFinishedLatch = new CountDownLatch(1);

        doAnswer(invocation -> {
            jobReachedSendLatch.countDown();
            producerFinishedLatch.await(10, TimeUnit.SECONDS);
            return invocation.callRealMethod();
        }).when(notificationEmailService).sendDigestEmail(anyString(), any());

        Thread jobThread = new Thread(digestJobService::sendDailyDigests);
        jobThread.start();

        assertThat(jobReachedSendLatch.await(10, TimeUnit.SECONDS)).isTrue();

        eventProcessingService.processNewEvent(payload(eventIdB));

        producerFinishedLatch.countDown();
        jobThread.join(TimeUnit.SECONDS.toMillis(30));
        assertThat(jobThread.isAlive()).isFalse();

        List<DigestQueue> remaining = digestQueueRepository.findByWatchId(watchId);
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().getEventId()).isEqualTo(eventIdB);

        NotificationLog logA = notificationLogRepository.findByWatchIdAndEventId(watchId, eventIdA).orElseThrow();
        assertThat(logA.getDeliveredAt()).isNotNull();

        NotificationLog logB = notificationLogRepository.findByWatchIdAndEventId(watchId, eventIdB).orElseThrow();
        assertThat(logB.getDeliveredAt()).isNull();
    }

    private NewEventPayloadDto payload(String eventId) {
        NewEventPayloadDto payload = new NewEventPayloadDto();
        payload.setEventId(eventId);
        payload.setTitle("Digest race test event " + eventId);
        payload.setCategories(List.of(CategoryEnum.WILDFIRES));
        payload.setEventDate(OffsetDateTime.now());
        payload.setIngestedAt(OffsetDateTime.now());
        return payload;
    }

    private BriefingResponseDto briefingResponse() {
        BriefingResponseDto briefing = new BriefingResponseDto();
        briefing.setSummary("Test summary");
        briefing.setImpact("Test impact");
        briefing.setSeverity(Severity.HIGH);
        briefing.setPrecautions(List.of("Stay alert"));
        return briefing;
    }
}

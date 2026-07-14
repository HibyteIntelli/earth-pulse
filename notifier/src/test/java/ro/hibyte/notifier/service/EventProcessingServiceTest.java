package ro.hibyte.notifier.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventProcessingServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private DigestQueueRepository digestQueueRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private LlmServiceClient llmServiceClient;

    @Mock
    private NotificationEmailService notificationEmailService;

    private EventProcessingService service;

    @BeforeEach
    void setUp() {
        service = new EventProcessingService(notificationLogRepository, digestQueueRepository, authServiceClient, llmServiceClient, notificationEmailService);
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:4200");
    }

    private NewEventPayloadDto payload() {
        NewEventPayloadDto payload = new NewEventPayloadDto();
        payload.setEventId("EONET_1");
        payload.setTitle("Wildfire near Test County");
        payload.setCategories(List.of(CategoryEnum.WILDFIRES));
        payload.setEventDate(OffsetDateTime.now());
        payload.setIngestedAt(OffsetDateTime.now());
        return payload;
    }

    private MatchedWatchDto watch(DeliveryMode mode) {
        MatchedWatchDto watch = new MatchedWatchDto();
        watch.setWatchId(UUID.randomUUID());
        watch.setUserId(UUID.randomUUID());
        watch.setUserEmail("user@example.com");
        watch.setDigestMode(mode);
        watch.setReadingLevel(ReadingLevel.DEFAULT);
        return watch;
    }

    private BriefingResponseDto briefingResponse() {
        BriefingResponseDto briefing = new BriefingResponseDto();
        briefing.setSummary("A wildfire was detected.");
        briefing.setImpact("Low impact on population.");
        briefing.setSeverity(Severity.HIGH);
        briefing.setPrecautions(List.of("Stay indoors"));
        return briefing;
    }

    @Test
    void immediateWatch_successfulSend_claimsThenUpdatesLogWithDeliveredAt() {
        NewEventPayloadDto payload = payload();
        MatchedWatchDto watch = watch(DeliveryMode.IMMEDIATE);

        when(authServiceClient.matchWatches(payload)).thenReturn(List.of(watch));
        when(notificationLogRepository.existsByWatchIdAndEventId(watch.getWatchId(), payload.getEventId())).thenReturn(false);
        when(llmServiceClient.fetchBriefing(eq(payload.getEventId()), eq(watch.getReadingLevel()))).thenReturn(briefingResponse());
        when(notificationLogRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.processNewEvent(payload);

        verify(notificationEmailService).sendImmediateEmail(eq(watch), eq(payload), any(), anyString());

        InOrder inOrder = inOrder(notificationLogRepository, notificationEmailService);
        inOrder.verify(notificationLogRepository).saveAndFlush(any());
        inOrder.verify(notificationEmailService).sendImmediateEmail(eq(watch), eq(payload), any(), anyString());
        inOrder.verify(notificationLogRepository).save(any());

        ArgumentCaptor<NotificationLog> updateCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getDeliveredAt()).isNotNull();
        assertThat(updateCaptor.getValue().getDeliveryMode()).isEqualTo(DeliveryMode.IMMEDIATE);
        verify(notificationLogRepository, never()).delete(any());
    }

    @Test
    void immediateWatch_sendFails_releasesClaimForRetry() {
        NewEventPayloadDto payload = payload();
        MatchedWatchDto watch = watch(DeliveryMode.IMMEDIATE);

        when(authServiceClient.matchWatches(payload)).thenReturn(List.of(watch));
        when(notificationLogRepository.existsByWatchIdAndEventId(watch.getWatchId(), payload.getEventId())).thenReturn(false);
        when(llmServiceClient.fetchBriefing(eq(payload.getEventId()), eq(watch.getReadingLevel()))).thenReturn(briefingResponse());
        when(notificationLogRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("SMTP down")).when(notificationEmailService)
                .sendImmediateEmail(eq(watch), eq(payload), any(), anyString());

        service.processNewEvent(payload);

        verify(notificationLogRepository).saveAndFlush(any());
        verify(notificationLogRepository).delete(any());
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void concurrentDuplicateClaim_skipsEmailEntirely() {
        NewEventPayloadDto payload = payload();
        MatchedWatchDto watch = watch(DeliveryMode.IMMEDIATE);

        when(authServiceClient.matchWatches(payload)).thenReturn(List.of(watch));
        when(notificationLogRepository.existsByWatchIdAndEventId(watch.getWatchId(), payload.getEventId())).thenReturn(false);
        when(llmServiceClient.fetchBriefing(eq(payload.getEventId()), eq(watch.getReadingLevel()))).thenReturn(briefingResponse());
        when(notificationLogRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        service.processNewEvent(payload);

        verifyNoInteractions(notificationEmailService);
        verify(notificationLogRepository, never()).save(any());
        verify(notificationLogRepository, never()).delete(any());
    }

    @Test
    void digestWatch_claimsUndeliveredLog_buffersDigestEntry_noEmailSent() {
        NewEventPayloadDto payload = payload();
        MatchedWatchDto watch = watch(DeliveryMode.DAILY_DIGEST);

        when(authServiceClient.matchWatches(payload)).thenReturn(List.of(watch));
        when(notificationLogRepository.existsByWatchIdAndEventId(watch.getWatchId(), payload.getEventId())).thenReturn(false);
        when(llmServiceClient.fetchBriefing(eq(payload.getEventId()), eq(watch.getReadingLevel()))).thenReturn(briefingResponse());
        when(notificationLogRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.processNewEvent(payload);

        verifyNoInteractions(notificationEmailService);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getDeliveredAt()).isNull();
        assertThat(captor.getValue().getDeliveryMode()).isEqualTo(DeliveryMode.DAILY_DIGEST);
        verify(notificationLogRepository, never()).save(any());
        verify(notificationLogRepository, never()).delete(any());

        ArgumentCaptor<DigestQueue> digestCaptor = ArgumentCaptor.forClass(DigestQueue.class);
        verify(digestQueueRepository).save(digestCaptor.capture());
        DigestQueue digestEntry = digestCaptor.getValue();
        assertThat(digestEntry.getWatchId()).isEqualTo(watch.getWatchId());
        assertThat(digestEntry.getEventId()).isEqualTo(payload.getEventId());
        assertThat(digestEntry.getUserId()).isEqualTo(watch.getUserId());
        assertThat(digestEntry.getUserEmail()).isEqualTo(watch.getUserEmail());
        assertThat(digestEntry.getReadingLevel()).isEqualTo(watch.getReadingLevel());
        assertThat(digestEntry.getMatchedAt()).isNotNull();
    }

    @Test
    void digestWatch_bufferingFails_releasesClaimForRetry() {
        NewEventPayloadDto payload = payload();
        MatchedWatchDto watch = watch(DeliveryMode.DAILY_DIGEST);

        when(authServiceClient.matchWatches(payload)).thenReturn(List.of(watch));
        when(notificationLogRepository.existsByWatchIdAndEventId(watch.getWatchId(), payload.getEventId())).thenReturn(false);
        when(llmServiceClient.fetchBriefing(eq(payload.getEventId()), eq(watch.getReadingLevel()))).thenReturn(briefingResponse());
        when(notificationLogRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("DB blip")).when(digestQueueRepository).save(any());

        service.processNewEvent(payload);

        verifyNoInteractions(notificationEmailService);
        verify(notificationLogRepository).saveAndFlush(any());
        verify(notificationLogRepository).delete(any());
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void alreadyDelivered_watchIsSkippedEntirely() {
        NewEventPayloadDto payload = payload();
        MatchedWatchDto watch = watch(DeliveryMode.IMMEDIATE);

        when(authServiceClient.matchWatches(payload)).thenReturn(List.of(watch));
        when(notificationLogRepository.existsByWatchIdAndEventId(watch.getWatchId(), payload.getEventId())).thenReturn(true);

        service.processNewEvent(payload);

        verifyNoInteractions(llmServiceClient);
        verifyNoInteractions(notificationEmailService);
        verify(notificationLogRepository, never()).saveAndFlush(any());
        verify(notificationLogRepository, never()).save(any());
    }
}

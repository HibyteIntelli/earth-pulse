package ro.hibyte.notifier.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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
class DigestJobServiceTest {

    @Mock
    private DigestQueueRepository digestQueueRepository;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private NotificationEmailService notificationEmailService;

    private DigestJobService service;

    @BeforeEach
    void setUp() {
        service = new DigestJobService(digestQueueRepository, notificationLogRepository, notificationEmailService);
        ReflectionTestUtils.setField(service, "self", service);
    }

    private DigestQueue digestEntry(UUID watchId, String eventId, String userEmail) {
        return DigestQueue.builder()
                .id(UUID.randomUUID())
                .watchId(watchId)
                .eventId(eventId)
                .userId(UUID.randomUUID())
                .userEmail(userEmail)
                .readingLevel(ReadingLevel.DEFAULT)
                .matchedAt(OffsetDateTime.now())
                .build();
    }

    private NotificationLog notificationLog(UUID watchId, String eventId) {
        return NotificationLog.builder()
                .id(UUID.randomUUID())
                .watchId(watchId)
                .userId(UUID.randomUUID())
                .eventId(eventId)
                .eventTitle("Wildfire near " + eventId)
                .eventCategories(List.of("wildfires"))
                .eventUrl("http://localhost:4200/events/" + eventId)
                .eventDate(OffsetDateTime.now())
                .deliveryMode(DeliveryMode.DAILY)
                .readingLevel(ReadingLevel.DEFAULT)
                .briefingSummary("Summary for " + eventId)
                .briefingImpact("Impact for " + eventId)
                .briefingSeverity(Severity.HIGH)
                .briefingPrecautions(List.of("Stay alert"))
                .build();
    }

    @Test
    void noPendingWatches_doesNothing() {
        when(digestQueueRepository.findDistinctWatchIds()).thenReturn(List.of());

        service.sendDailyDigests();

        verifyNoInteractions(notificationEmailService);
        verify(notificationLogRepository, never()).saveAll(any());
        verify(digestQueueRepository, never()).deleteAll(any());
    }

    @Test
    void watchWithPendingMatches_sendsDigestMarksDeliveredAndDeletesExactRows() {
        UUID watchId = UUID.randomUUID();
        DigestQueue entryA = digestEntry(watchId, "EONET_A", "user@example.com");
        DigestQueue entryB = digestEntry(watchId, "EONET_B", "user@example.com");
        NotificationLog logA = notificationLog(watchId, "EONET_A");
        NotificationLog logB = notificationLog(watchId, "EONET_B");

        when(digestQueueRepository.findDistinctWatchIds()).thenReturn(List.of(watchId));
        when(digestQueueRepository.findByWatchId(watchId)).thenReturn(List.of(entryA, entryB));
        when(notificationLogRepository.findByWatchIdAndEventIdIn(eq(watchId), any()))
                .thenReturn(List.of(logA, logB));

        service.sendDailyDigests();

        verify(notificationEmailService).sendDigestEmail(eq("user@example.com"), argThat(events ->
                events.size() == 2 && events.containsAll(List.of(logA, logB))));

        ArgumentCaptor<List<NotificationLog>> savedCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationLogRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).allSatisfy(n -> assertThat(n.getDeliveredAt()).isNotNull());

        ArgumentCaptor<List<DigestQueue>> deletedCaptor = ArgumentCaptor.forClass(List.class);
        verify(digestQueueRepository).deleteAll(deletedCaptor.capture());
        assertThat(deletedCaptor.getValue()).containsExactlyInAnyOrder(entryA, entryB);
    }

    @Test
    void digestSendFails_leavesEverythingQueuedForRetry() {
        UUID watchId = UUID.randomUUID();
        DigestQueue entry = digestEntry(watchId, "EONET_A", "user@example.com");
        NotificationLog log = notificationLog(watchId, "EONET_A");

        when(digestQueueRepository.findDistinctWatchIds()).thenReturn(List.of(watchId));
        when(digestQueueRepository.findByWatchId(watchId)).thenReturn(List.of(entry));
        when(notificationLogRepository.findByWatchIdAndEventIdIn(eq(watchId), any())).thenReturn(List.of(log));
        doThrow(new RuntimeException("SMTP down")).when(notificationEmailService).sendDigestEmail(anyString(), any());

        service.sendDailyDigests();

        verify(notificationLogRepository, never()).saveAll(any());
        verify(digestQueueRepository, never()).deleteAll(any());
    }

    @Test
    void orphanedDigestEntry_withNoMatchingNotificationLog_isCleanedUpWithoutSendingEmail() {
        UUID watchId = UUID.randomUUID();
        DigestQueue entry = digestEntry(watchId, "EONET_GHOST", "user@example.com");

        when(digestQueueRepository.findDistinctWatchIds()).thenReturn(List.of(watchId));
        when(digestQueueRepository.findByWatchId(watchId)).thenReturn(List.of(entry));
        when(notificationLogRepository.findByWatchIdAndEventIdIn(eq(watchId), any())).thenReturn(List.of());

        service.sendDailyDigests();

        verifyNoInteractions(notificationEmailService);
        verify(notificationLogRepository, never()).saveAll(any());
        verify(digestQueueRepository).deleteAll(List.of(entry));
    }

    @Test
    void multipleWatches_eachHandledIndependently() {
        UUID watchIdOk = UUID.randomUUID();
        UUID watchIdFails = UUID.randomUUID();
        DigestQueue entryOk = digestEntry(watchIdOk, "EONET_OK", "ok@example.com");
        DigestQueue entryFails = digestEntry(watchIdFails, "EONET_FAIL", "fail@example.com");
        NotificationLog logOk = notificationLog(watchIdOk, "EONET_OK");
        NotificationLog logFails = notificationLog(watchIdFails, "EONET_FAIL");

        when(digestQueueRepository.findDistinctWatchIds()).thenReturn(List.of(watchIdOk, watchIdFails));
        when(digestQueueRepository.findByWatchId(watchIdOk)).thenReturn(List.of(entryOk));
        when(digestQueueRepository.findByWatchId(watchIdFails)).thenReturn(List.of(entryFails));
        when(notificationLogRepository.findByWatchIdAndEventIdIn(eq(watchIdOk), any())).thenReturn(List.of(logOk));
        when(notificationLogRepository.findByWatchIdAndEventIdIn(eq(watchIdFails), any())).thenReturn(List.of(logFails));
        doNothing().when(notificationEmailService).sendDigestEmail(eq("ok@example.com"), any());
        doThrow(new RuntimeException("SMTP down")).when(notificationEmailService)
                .sendDigestEmail(eq("fail@example.com"), any());

        service.sendDailyDigests();

        verify(notificationEmailService).sendDigestEmail(eq("ok@example.com"), any());
        verify(digestQueueRepository).deleteAll(List.of(entryOk));
        verify(digestQueueRepository, never()).deleteAll(List.of(entryFails));
    }
}

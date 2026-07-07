package ro.hibyte.notifier.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ro.hibyte.notifier.entity.DigestQueue;
import ro.hibyte.notifier.entity.NotificationLog;
import ro.hibyte.notifier.repository.DigestQueueRepository;
import ro.hibyte.notifier.repository.NotificationLogRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigestJobService {

    private final DigestQueueRepository digestQueueRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationEmailService notificationEmailService;

    @Scheduled(cron = "${notifier.digest.cron}")
    public void sendDailyDigests() {
        List<UUID> watchIds = digestQueueRepository.findDistinctWatchIds();
        log.info("Running daily digest job for {} watch(es) with pending matches", watchIds.size());

        for (UUID watchId : watchIds) {
            sendDigestForWatch(watchId);
        }
    }

    private void sendDigestForWatch(UUID watchId) {
        List<DigestQueue> pending = digestQueueRepository.findByWatchId(watchId);
        if (pending.isEmpty()) {
            return;
        }

        List<NotificationLog> events = pending.stream()
                .map(entry -> notificationLogRepository.findByWatchIdAndEventId(entry.getWatchId(), entry.getEventId()))
                .flatMap(Optional::stream)
                .toList();

        if (!events.isEmpty()) {
            try {
                notificationEmailService.sendDigestEmail(pending.getFirst().getUserEmail(), events);
            } catch (Exception e) {
                log.error("Failed to send daily digest for watch={}: {}. Leaving {} match(es) queued for the next run.",
                        watchId, e.getMessage(), pending.size());
                return;
            }

            OffsetDateTime now = OffsetDateTime.now();
            events.forEach(n -> n.setDeliveredAt(now));
            notificationLogRepository.saveAll(events);
        } else {
            log.warn("Digest queue entries for watch={} have no matching NotificationLog rows ({} orphaned entr{})",
                    watchId, pending.size(), pending.size() == 1 ? "y" : "ies");
        }

        digestQueueRepository.deleteAll(pending);
    }
}

package ro.hibyte.notifier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.hibyte.notifier.entity.NotificationLog;

import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    boolean existsByWatchIdAndEventId(UUID watchId, String eventId);

    List<NotificationLog> findByUserIdOrderByDeliveredAtDesc(UUID userId);
}

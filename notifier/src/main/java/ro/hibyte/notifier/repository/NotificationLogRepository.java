package ro.hibyte.notifier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.hibyte.notifier.entity.NotificationLog;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    boolean existsByWatchIdAndEventId(Long watchId, String eventId);

    List<NotificationLog> findByUserIdOrderByDeliveredAtDesc(Long userId);
}

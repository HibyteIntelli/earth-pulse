package ro.hibyte.notifier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.hibyte.notifier.entity.DigestQueue;

import java.util.List;
import java.util.UUID;

public interface DigestQueueRepository extends JpaRepository<DigestQueue, UUID> {

    boolean existsByWatchIdAndEventId(UUID watchId, String eventId);

    List<DigestQueue> findByWatchId(UUID watchId);

    void deleteByWatchId(UUID watchId);
}

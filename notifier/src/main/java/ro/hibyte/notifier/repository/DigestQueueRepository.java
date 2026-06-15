package ro.hibyte.notifier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.hibyte.notifier.entity.DigestQueue;

import java.util.List;

public interface DigestQueueRepository extends JpaRepository<DigestQueue, Long> {

    boolean existsByWatchIdAndEventId(Long watchId, String eventId);

    List<DigestQueue> findByWatchId(Long watchId);

    void deleteByWatchId(Long watchId);
}

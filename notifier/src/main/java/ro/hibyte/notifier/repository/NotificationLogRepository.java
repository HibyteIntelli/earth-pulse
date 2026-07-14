package ro.hibyte.notifier.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.hibyte.notifier.entity.DeliveryMode;
import ro.hibyte.notifier.entity.NotificationLog;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    boolean existsByWatchIdAndEventId(UUID watchId, String eventId);

    @Query("SELECT n FROM NotificationLog n LEFT JOIN FETCH n.briefingPrecautions " +
           "WHERE n.watchId = :watchId AND n.eventId = :eventId")
    Optional<NotificationLog> findByWatchIdAndEventId(@Param("watchId") UUID watchId, @Param("eventId") String eventId);

    @Query("SELECT DISTINCT n FROM NotificationLog n LEFT JOIN FETCH n.briefingPrecautions " +
           "WHERE n.watchId = :watchId AND n.eventId IN :eventIds")
    List<NotificationLog> findByWatchIdAndEventIdIn(@Param("watchId") UUID watchId, @Param("eventIds") Collection<String> eventIds);

    List<NotificationLog> findByUserIdAndDeliveredAtIsNotNullOrderByDeliveredAtDesc(UUID userId);

    @Query("SELECT DISTINCT n FROM NotificationLog n WHERE n.userId = :userId " +
           "AND n.deliveredAt IS NOT NULL " +
           "AND (:eventId IS NULL OR n.eventId = :eventId) " +
           "AND (:category IS NULL OR :category MEMBER OF n.eventCategories) " +
           "AND (:deliveryMode IS NULL OR n.deliveryMode = :deliveryMode) " +
           "AND (:since IS NULL OR n.deliveredAt >= :since)")
    Page<NotificationLog> findByFilters(
            @Param("userId") UUID userId,
            @Param("eventId") String eventId,
            @Param("category") String category,
            @Param("deliveryMode") DeliveryMode deliveryMode,
            @Param("since") OffsetDateTime since,
            Pageable pageable);
}

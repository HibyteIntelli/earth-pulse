package ro.hibyte.notifier.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "notification_log",
        uniqueConstraints = @UniqueConstraint(columnNames = {"watch_id", "event_id"}),
        indexes = @Index(name = "idx_notification_log_user_delivered", columnList = "user_id, delivered_at DESC")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "watch_id", nullable = false)
    private UUID watchId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_title", nullable = false, length = 255)
    private String eventTitle;

    @Column(name = "event_category", nullable = false, length = 100)
    private String eventCategory;

    @Column(name = "event_url", nullable = false, length = 500)
    private String eventUrl;

    @Column(name = "event_date", nullable = false)
    private OffsetDateTime eventDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false)
    private DeliveryMode deliveryMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_level", nullable = false)
    private ReadingLevel readingLevel;

    @Column(name = "delivered_at", nullable = false)
    private OffsetDateTime deliveredAt;

    @Column(name = "briefing_summary", nullable = false, columnDefinition = "TEXT")
    private String briefingSummary;

    @Column(name = "briefing_impact", nullable = false, columnDefinition = "TEXT")
    private String briefingImpact;

    @Enumerated(EnumType.STRING)
    @Column(name = "briefing_severity", nullable = false)
    private Severity briefingSeverity;

    @ElementCollection
    @CollectionTable(name = "notification_log_precautions", joinColumns = @JoinColumn(name = "notification_id"))
    @Column(name = "precaution", columnDefinition = "TEXT")
    private List<String> briefingPrecautions;
}

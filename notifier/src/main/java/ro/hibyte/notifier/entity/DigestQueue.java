package ro.hibyte.notifier.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "digest_queue",
        uniqueConstraints = @UniqueConstraint(columnNames = {"watch_id", "event_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigestQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "watch_id", nullable = false)
    private UUID watchId;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_level", nullable = false)
    private ReadingLevel readingLevel;

    @Column(name = "matched_at", nullable = false)
    private OffsetDateTime matchedAt;
}

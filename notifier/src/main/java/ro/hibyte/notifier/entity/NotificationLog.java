package ro.hibyte.notifier.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "notification_log",
        uniqueConstraints = @UniqueConstraint(columnNames = {"watch_id", "event_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "watch_id", nullable = false)
    private Long watchId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_title", nullable = false, length = 255)
    private String eventTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false)
    private DeliveryMode deliveryMode;

    @Column(name = "delivered_at", nullable = false)
    private OffsetDateTime deliveredAt;
}

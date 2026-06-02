package ro.hibyte.ingestion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "natural_events")
public class NaturalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String eonetId;

    @Column(nullable = false)
    private String title;

    private String description;

    private String link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    private OffsetDateTime closedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "event_categories",
            joinColumns = @JoinColumn(name = "event_id")
    )
    @Column(name = "category_id")
    private Set<String> categoryIds = new HashSet<>();

    private Double longitude;
    private Double latitude;

    private OffsetDateTime eventDate;

    private Double magnitudeValue;
    private String magnitudeUnit;
}

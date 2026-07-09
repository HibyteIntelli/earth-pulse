package ro.hibyte.ingestion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ro.hibyte.ingestion.dto.eonet.EonetCategory;
import ro.hibyte.ingestion.dto.eonet.EonetEvent;
import ro.hibyte.ingestion.dto.eonet.EonetGeometry;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "natural_events")
public class Event {

    @Id
    @Column(nullable = false)
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
            joinColumns = @JoinColumn(name = "eonet_id")
    )
    @Column(name = "category_id")
    private Set<String> categoryIds = new HashSet<>();

    private Double longitude;
    private Double latitude;

    private OffsetDateTime eventDate;

    private Double magnitudeValue;
    private String magnitudeUnit;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime ingestedAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public void applyFields(EonetEvent eonetEvent) {
        setTitle(eonetEvent.getTitle());
        setDescription(eonetEvent.getDescription());
        setLink(eonetEvent.getLink());

        setStatus(eonetEvent.getClosed() == null ? EventStatus.OPEN : EventStatus.CLOSED);
        setClosedAt(eonetEvent.getClosed());

        if (eonetEvent.getCategories() != null) {
            Set<String> categoryIds = eonetEvent.getCategories().stream()
                    .map(EonetCategory::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            setCategoryIds(categoryIds);
        }

        List<EonetGeometry> geometries = eonetEvent.getGeometry();
        if (geometries != null && !geometries.isEmpty()) {
            EonetGeometry latest = geometries.getLast();
            setEventDate(latest.getDate());
            setMagnitudeValue(latest.getMagnitudeValue());
            setMagnitudeUnit(latest.getMagnitudeUnit());

            List<Object> coords = latest.getCoordinates();
            if (coords != null && coords.size() >= 2
                    && coords.get(0) instanceof Number lon
                    && coords.get(1) instanceof Number lat) {
                setLongitude(lon.doubleValue());
                setLatitude(lat.doubleValue());
            }
        }
    }
}

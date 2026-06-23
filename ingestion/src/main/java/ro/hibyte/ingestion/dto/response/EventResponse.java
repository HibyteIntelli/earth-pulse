package ro.hibyte.ingestion.dto.response;

import lombok.Data;
import ro.hibyte.ingestion.model.Event;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class EventResponse {

    private String id;
    private String title;
    private String description;
    private String sourceUrl;
    private String status;
    private OffsetDateTime closedAt;
    private List<String> category;
    private Geometry geometry;
    private OffsetDateTime eventDate;
    private Double magnitudeValue;
    private String magnitudeUnit;
    private OffsetDateTime ingestedAt;
    private OffsetDateTime updatedAt;

    public EventResponse(Event event) {
        this.id = event.getEonetId();
        this.title = event.getTitle();
        this.description = event.getDescription();
        this.sourceUrl = event.getLink();
        this.status = event.getStatus() != null ? event.getStatus().name().toLowerCase() : null;
        this.closedAt = event.getClosedAt();
        this.category = new ArrayList<>(event.getCategoryIds());
        this.eventDate = event.getEventDate();
        this.magnitudeValue = event.getMagnitudeValue();
        this.magnitudeUnit = event.getMagnitudeUnit();
        this.ingestedAt = event.getIngestedAt();
        this.updatedAt = event.getUpdatedAt();

        if (event.getLongitude() != null && event.getLatitude() != null) {
            this.geometry = new Geometry(event.getLongitude(), event.getLatitude());
        }
    }

}

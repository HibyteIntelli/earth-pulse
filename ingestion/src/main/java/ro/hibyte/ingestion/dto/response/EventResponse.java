package ro.hibyte.ingestion.dto.response;

import lombok.Data;
import ro.hibyte.ingestion.model.EventStatus;

import java.time.OffsetDateTime;
import java.util.Set;

@Data
public class EventResponse {

    private String eonetId;
    private String title;
    private String description;
    private String link;
    private EventStatus status;
    private OffsetDateTime closedAt;
    private Set<String> categoryIds;
    private Double latitude;
    private Double longitude;
    private OffsetDateTime eventDate;
    private Double magnitudeValue;
    private String magnitudeUnit;
}

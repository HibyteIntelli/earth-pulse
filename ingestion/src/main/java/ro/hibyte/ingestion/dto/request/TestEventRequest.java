package ro.hibyte.ingestion.dto.request;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Data;

import ro.hibyte.ingestion.dto.eonet.EonetCategory;
import ro.hibyte.ingestion.dto.eonet.EonetEvent;
import ro.hibyte.ingestion.dto.eonet.EonetGeometry;
import ro.hibyte.ingestion.exception.ErrorCode;
import ro.hibyte.ingestion.exception.InvalidFilterException;

@Data
public class TestEventRequest {

    private String eonetId;
    private String title;
    private Double longitude;
    private Double latitude;
    private OffsetDateTime eventDate;

    private String description;
    private String link;
    private List<String> categoryIds;
    private Double magnitudeValue;
    private String magnitudeUnit;
    private OffsetDateTime closed;

    public EonetEvent toEonetEvent() {
        if (eonetId == null) {
            throw new InvalidFilterException(ErrorCode.MALFORMED_REQUEST, "eonetId is required");
        }
        if (title == null) {
            throw new InvalidFilterException(ErrorCode.MALFORMED_REQUEST, "title is required");
        }
        if (longitude == null) {
            throw new InvalidFilterException(ErrorCode.MALFORMED_REQUEST, "longitude is required");
        }
        if (latitude == null) {
            throw new InvalidFilterException(ErrorCode.MALFORMED_REQUEST, "latitude is required");
        }
        if (eventDate == null) {
            throw new InvalidFilterException(ErrorCode.MALFORMED_REQUEST, "eventDate is required");
        }

        EonetGeometry geometry = new EonetGeometry();
        geometry.setDate(eventDate);
        geometry.setMagnitudeValue(magnitudeValue);
        geometry.setMagnitudeUnit(magnitudeUnit);
        geometry.setCoordinates(List.of(longitude, latitude));

        List<EonetCategory> categories = categoryIds == null ? null :
                categoryIds.stream().map(id -> {
                    EonetCategory c = new EonetCategory();
                    c.setId(id);
                    return c;
                }).toList();

        EonetEvent event = new EonetEvent();
        event.setId(eonetId);
        event.setTitle(title);
        event.setDescription(description);
        event.setLink(link);
        event.setClosed(closed);
        event.setCategories(categories);
        event.setGeometry(List.of(geometry));
        return event;
    }
}

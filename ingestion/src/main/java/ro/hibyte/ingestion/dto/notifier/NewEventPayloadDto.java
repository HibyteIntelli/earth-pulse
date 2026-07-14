package ro.hibyte.ingestion.dto.notifier;

import lombok.Data;
import ro.hibyte.ingestion.model.Event;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class NewEventPayloadDto {
    private String eventId;
    private String title;
    private List<String> categories;
    private GeometryDto geometry;
    private Double magnitudeValue;
    private String magnitudeUnit;
    private OffsetDateTime eventDate;
    private OffsetDateTime ingestedAt;

    public NewEventPayloadDto(Event event) {
        this.eventId = event.getEonetId();
        this.title = event.getTitle();
        this.categories = event.getCategoryIds().stream().sorted().toList();
        this.magnitudeValue = event.getMagnitudeValue();
        this.magnitudeUnit = event.getMagnitudeUnit();
        this.eventDate = event.getEventDate();
        this.ingestedAt = event.getIngestedAt();

        GeometryDto geometryDto = new GeometryDto();
        geometryDto.setType("Point");
        geometryDto.setCoordinates(List.of(event.getLongitude(), event.getLatitude()));
        this.geometry = geometryDto;
    }
}

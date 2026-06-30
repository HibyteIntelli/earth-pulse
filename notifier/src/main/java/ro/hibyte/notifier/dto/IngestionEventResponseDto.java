package ro.hibyte.notifier.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class IngestionEventResponseDto {

    private String id;
    private String title;
    private String description;
    private String sourceUrl;
    private String status;
    private OffsetDateTime closedAt;
    private List<String> category;
    private GeometryDto geometry;
    private OffsetDateTime eventDate;
    private Double magnitudeValue;
    private String magnitudeUnit;
    private OffsetDateTime ingestedAt;
    private OffsetDateTime updatedAt;
}

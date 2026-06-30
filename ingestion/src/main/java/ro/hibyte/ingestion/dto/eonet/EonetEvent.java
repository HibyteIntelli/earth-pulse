package ro.hibyte.ingestion.dto.eonet;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class EonetEvent {
    private String id;
    private String title;
    private String description;
    private String link;
    private OffsetDateTime closed;
    private List<EonetCategory> categories;
    private List<EonetGeometry> geometry;
}

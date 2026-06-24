package ro.hibyte.ingestion.dto.eonet;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class EonetGeometry {
    private Double magnitudeValue;
    private String magnitudeUnit;
    private OffsetDateTime date;
    private String type;
    private List<Object> coordinates;
}

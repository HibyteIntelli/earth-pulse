package ro.hibyte.ingestion.dto.eonet;

import lombok.Data;

import java.util.List;

@Data
public class EonetResponse {
    private List<EonetEvent> events;
}

package ro.hibyte.ingestion.dto.request;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class EventFilter {
    private String bbox;
    private List<String> category;
    private String status;
    private OffsetDateTime start;
    private OffsetDateTime end;
    private OffsetDateTime since;
    private String sort;
    private Integer limit;
    private Integer offset;
}

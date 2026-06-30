package ro.hibyte.ingestion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventPage {

    private List<EventResponse> items;
    private int total;
    private int limit;
    private int offset;
}

package ro.hibyte.notifier.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EventMatchRequestDto {

    private String eventId;
    private List<String> categories;
    private GeometryDto point;
}

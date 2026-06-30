package ro.hibyte.notifier.dto;

import lombok.Data;

import java.util.List;

@Data
public class EventMatchResponseDto {

    private List<MatchedWatchDto> matches;
}

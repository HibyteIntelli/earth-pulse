package ro.hibyte.notifier.dto;

import lombok.Data;

import java.util.List;

@Data
public class IngestionEventPageDto {

    private List<IngestionEventResponseDto> items;
    private int total;
    private int page;
    private int size;
}

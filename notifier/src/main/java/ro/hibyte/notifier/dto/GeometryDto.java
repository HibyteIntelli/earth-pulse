package ro.hibyte.notifier.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class GeometryDto {

    @NotNull
    private String type;

    @NotNull
    @Size(min = 2, max = 2)
    private List<Double> coordinates;
}

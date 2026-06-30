package ro.hibyte.notifier.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import ro.hibyte.notifier.entity.CategoryEnum;
@Data
public class NewEventPayloadDto {

    @NotBlank
    private String eventId;

    @NotBlank
    private String title;

    @NotEmpty
    @NotEmpty
    private List<CategoryEnum> categories;

    @NotNull
    @Valid
    private GeometryDto geometry;

    private Double magnitudeValue;
    private String magnitudeUnit;

    @NotNull
    private OffsetDateTime eventDate;

    @NotNull
    private OffsetDateTime ingestedAt;
}

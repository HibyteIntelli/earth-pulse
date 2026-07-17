package ro.hibyte.ingestion.dto.notifier;

import org.junit.jupiter.api.Test;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.support.EventTestData;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewEventPayloadDtoTest {

    @Test
    void sortsCategories() {
        Event event = EventTestData.anEvent().geometry(10.0, 20.0).category("c", "a", "b").build();

        NewEventPayloadDto payload = new NewEventPayloadDto(event);

        assertThat(payload.getCategories()).containsExactly("a", "b", "c");
    }

    @Test
    void copiesCoreFieldsAndBuildsPointGeometry() {
        OffsetDateTime date = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        Event event = EventTestData.anEvent().eonetId("E1").title("Quake")
                .geometry(10.0, 20.0).eventDate(date).magnitude(5.0, "mag").build();

        NewEventPayloadDto payload = new NewEventPayloadDto(event);

        assertThat(payload.getEventId()).isEqualTo("E1");
        assertThat(payload.getTitle()).isEqualTo("Quake");
        assertThat(payload.getEventDate()).isEqualTo(date);
        assertThat(payload.getMagnitudeValue()).isEqualTo(5.0);
        assertThat(payload.getGeometry().getType()).isEqualTo("Point");
        assertThat(payload.getGeometry().getCoordinates()).containsExactly(10.0, 20.0);
    }

    @Test
    void throwsWhenCoordinatesNull() {
        Event event = EventTestData.anEvent().build();

        assertThatThrownBy(() -> new NewEventPayloadDto(event))
                .isInstanceOf(NullPointerException.class);
    }
}

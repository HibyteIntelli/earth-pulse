package ro.hibyte.ingestion.model;

import org.junit.jupiter.api.Test;
import ro.hibyte.ingestion.support.EventTestData;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventTest {

    @Test
    void mapsOpenStatusWhenNotClosed() {
        Event event = new Event();
        event.applyFields(EventTestData.anEonetEvent().build());

        assertThat(event.getStatus()).isEqualTo(EventStatus.OPEN);
        assertThat(event.getClosedAt()).isNull();
    }

    @Test
    void setsClosedStatusAndClosedAt() {
        OffsetDateTime closedAt = OffsetDateTime.parse("2024-01-01T00:00:00Z");
        Event event = new Event();
        event.applyFields(EventTestData.anEonetEvent().closed(closedAt).build());

        assertThat(event.getStatus()).isEqualTo(EventStatus.CLOSED);
        assertThat(event.getClosedAt()).isEqualTo(closedAt);
    }

    @Test
    void mapsCategoriesFilteringNulls() {
        Event event = new Event();
        event.applyFields(EventTestData.anEonetEvent().category("wildfires", null).build());

        assertThat(event.getCategoryIds()).containsExactly("wildfires");
    }

    @Test
    void leavesCategoriesEmptyWhenNull() {
        Event event = new Event();
        event.applyFields(EventTestData.anEonetEvent().build());

        assertThat(event.getCategoryIds()).isEmpty();
    }

    @Test
    void extractsCoordinatesFromValidGeometry() {
        Event event = new Event();
        event.applyFields(EventTestData.anEonetEvent().geometry(10.0, 20.0).build());

        assertThat(event.getLongitude()).isEqualTo(10.0);
        assertThat(event.getLatitude()).isEqualTo(20.0);
    }

    @Test
    void leavesCoordinatesUnsetWhenFewerThanTwo() {
        Event event = new Event();
        event.applyFields(EventTestData.anEonetEvent().geometryRaw(List.of(10.0)).build());

        assertThat(event.getLongitude()).isNull();
        assertThat(event.getLatitude()).isNull();
    }

    @Test
    void leavesCoordinatesUnsetWhenNonNumber() {
        Event event = new Event();
        event.applyFields(EventTestData.anEonetEvent().geometryRaw(List.of("a", "b")).build());

        assertThat(event.getLongitude()).isNull();
        assertThat(event.getLatitude()).isNull();
    }

    @Test
    void leavesGeometryUnsetWhenNoGeometry() {
        Event event = new Event();
        event.applyFields(EventTestData.anEonetEvent().build());

        assertThat(event.getLongitude()).isNull();
        assertThat(event.getLatitude()).isNull();
        assertThat(event.getEventDate()).isNull();
    }

    @Test
    void copiesDateAndMagnitudeFromGeometry() {
        OffsetDateTime date = OffsetDateTime.parse("2024-01-01T00:00:00Z");
        Event event = new Event();

        event.applyFields(EventTestData.anEonetEvent()
                .geometry(10.0, 20.0, date, 5.0, "kt").build());

        assertThat(event.getEventDate()).isEqualTo(date);
        assertThat(event.getMagnitudeValue()).isEqualTo(5.0);
        assertThat(event.getMagnitudeUnit()).isEqualTo("kt");
    }

    @Test
    void usesLastGeometryWhenMultiple() {
        OffsetDateTime dateA = OffsetDateTime.parse("2024-01-01T00:00:00Z");
        OffsetDateTime dateB = OffsetDateTime.parse("2024-06-01T00:00:00Z");
        Event event = new Event();
        event.applyFields(EventTestData.anEonetEvent()
                .geometry(1.0, 2.0, dateA, 3.0, "a")
                .geometry(10.0, 20.0, dateB, 5.0, "b")
                .build());

        assertThat(event.getEventDate()).isEqualTo(dateB);
        assertThat(event.getLongitude()).isEqualTo(10.0);
        assertThat(event.getMagnitudeUnit()).isEqualTo("b");
    }
}

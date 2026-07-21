package ro.hibyte.ingestion.dto.response;

import org.junit.jupiter.api.Test;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.support.EventTestData;

import static org.assertj.core.api.Assertions.assertThat;

class EventResponseTest {

    @Test
    void mapsBasicFieldsAndLowercasesStatus() {
        Event event = EventTestData.anEvent().eonetId("E1").title("Wildfire").category("wildfires").build();

        EventResponse response = new EventResponse(event);

        assertThat(response.getId()).isEqualTo("E1");
        assertThat(response.getTitle()).isEqualTo("Wildfire");
        assertThat(response.getStatus()).isEqualTo("open");
        assertThat(response.getCategory()).containsExactly("wildfires");
    }

    @Test
    void statusNullMapsToNull() {
        Event event = new Event();
        event.setEonetId("E1");

        EventResponse response = new EventResponse(event);
        assertThat(response.getStatus()).isNull();
    }

    @Test
    void buildsPointGeometryInLonLatOrder() {
        Event event = EventTestData.anEvent().geometry(10.0, 20.0).build();

        EventResponse response = new EventResponse(event);

        assertThat(response.getGeometry()).isNotNull();
        assertThat(response.getGeometry().getType()).isEqualTo("Point");
        assertThat(response.getGeometry().getCoordinates()).containsExactly(10.0, 20.0);
    }

    @Test
    void geometryNullWhenCoordinatesAbsent() {
        Event event = EventTestData.anEvent().build();

        EventResponse response = new EventResponse(event);

        assertThat(response.getGeometry()).isNull();
    }

    @Test
    void geometryNullWhenOnlyLongitudePresent() {
        Event event = new Event();
        event.setEonetId("E1");
        event.setLongitude(10.0);

        EventResponse response = new EventResponse(event);

        assertThat(response.getGeometry()).isNull();
    }
}

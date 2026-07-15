package ro.hibyte.ingestion.dto.request;

import org.junit.jupiter.api.Test;
import ro.hibyte.ingestion.dto.eonet.EonetCategory;
import ro.hibyte.ingestion.dto.eonet.EonetEvent;
import ro.hibyte.ingestion.dto.eonet.EonetGeometry;
import ro.hibyte.ingestion.exception.ErrorCode;
import ro.hibyte.ingestion.exception.InvalidFilterException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestEventRequestTest {

    private static final OffsetDateTime EVENT_DATE = OffsetDateTime.parse("2026-07-15T10:00:00Z");

    private TestEventRequest validRequest() {
        TestEventRequest request = new TestEventRequest();
        request.setEonetId("TEST-1");
        request.setTitle("Test wildfire");
        request.setLongitude(26.1);
        request.setLatitude(44.4);
        request.setEventDate(EVENT_DATE);
        request.setDescription("desc");
        request.setLink("http://example.test");
        request.setCategoryIds(List.of("wildfires", "severeStorms"));
        request.setMagnitudeValue(5.0);
        request.setMagnitudeUnit("MW");
        return request;
    }

    @Test
    void mapsAllFieldsToEonetEvent() {
        EonetEvent event = validRequest().toEonetEvent();

        assertThat(event.getId()).isEqualTo("TEST-1");
        assertThat(event.getTitle()).isEqualTo("Test wildfire");
        assertThat(event.getDescription()).isEqualTo("desc");
        assertThat(event.getLink()).isEqualTo("http://example.test");
        assertThat(event.getClosed()).isNull();

        assertThat(event.getCategories())
                .extracting(EonetCategory::getId)
                .containsExactly("wildfires", "severeStorms");

        assertThat(event.getGeometry()).hasSize(1);
        EonetGeometry geometry = event.getGeometry().getFirst();
        assertThat(geometry.getDate()).isEqualTo(EVENT_DATE);
        assertThat(geometry.getMagnitudeValue()).isEqualTo(5.0);
        assertThat(geometry.getMagnitudeUnit()).isEqualTo("MW");
    }

    @Test
    void mapsCoordinatesAsLongitudeThenLatitude() {
        EonetEvent event = validRequest().toEonetEvent();

        assertThat(event.getGeometry().getFirst().getCoordinates())
                .containsExactly(26.1, 44.4);
    }

    @Test
    void nullCategoryIdsProducesNullCategories() {
        TestEventRequest request = validRequest();
        request.setCategoryIds(null);

        assertThat(request.toEonetEvent().getCategories()).isNull();
    }

    @Test
    void throwsWhenEonetIdMissing() {
        TestEventRequest request = validRequest();
        request.setEonetId(null);

        assertThatThrownBy(request::toEonetEvent)
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("eonetId is required")
                .extracting(e -> ((InvalidFilterException) e).getCode())
                .isEqualTo(ErrorCode.MALFORMED_REQUEST);
    }

    @Test
    void throwsWhenTitleMissing() {
        TestEventRequest request = validRequest();
        request.setTitle(null);

        assertThatThrownBy(request::toEonetEvent)
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("title is required");
    }

    @Test
    void throwsWhenLongitudeMissing() {
        TestEventRequest request = validRequest();
        request.setLongitude(null);

        assertThatThrownBy(request::toEonetEvent)
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("longitude is required");
    }

    @Test
    void throwsWhenLatitudeMissing() {
        TestEventRequest request = validRequest();
        request.setLatitude(null);

        assertThatThrownBy(request::toEonetEvent)
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("latitude is required");
    }

    @Test
    void throwsWhenEventDateMissing() {
        TestEventRequest request = validRequest();
        request.setEventDate(null);

        assertThatThrownBy(request::toEonetEvent)
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("eventDate is required");
    }
}

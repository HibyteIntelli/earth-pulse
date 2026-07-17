package ro.hibyte.ingestion.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import ro.hibyte.ingestion.dto.request.CategoryEnum;
import ro.hibyte.ingestion.dto.request.EventFilter;
import ro.hibyte.ingestion.dto.request.StatusEnum;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.support.AbstractPostgresIT;
import ro.hibyte.ingestion.support.EventTestData;
import ro.hibyte.ingestion.validation.BoundingBox;
import ro.hibyte.ingestion.validation.ValidatedEventFilter;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventSpecificationTest extends AbstractPostgresIT {

    @Autowired
    private EventRepository eventRepository;

    private final EventSpecification eventSpecification = new EventSpecification();

    private List<Event> find(EventFilter filter, BoundingBox bbox) {
        ValidatedEventFilter validated = new ValidatedEventFilter(filter, bbox, 0, 100);
        return eventRepository.findAll(eventSpecification.build(validated));
    }

    private void saveOpenAndClosed() {
        eventRepository.save(EventTestData.anEvent().eonetId("OPEN_1").open().build());
        eventRepository.save(EventTestData.anEvent().eonetId("CLOSED_1")
                .closed(OffsetDateTime.parse("2026-01-01T00:00:00Z")).build());
    }

    @Test
    void statusDefaultsToOpen() {
        saveOpenAndClosed();

        EventFilter filter = new EventFilter();
        List<Event> events = find(filter, null);

        assertThat(events).extracting(Event::getEonetId).containsExactly("OPEN_1");
    }

    @Test
    void statusAllReturnsBoth() {
        saveOpenAndClosed();

        EventFilter filter = new EventFilter();
        filter.setStatus(StatusEnum.ALL);
        List<Event> events = find(filter, null);

        assertThat(events).extracting(Event::getEonetId).containsExactlyInAnyOrder("OPEN_1", "CLOSED_1");
    }

    @Test
    void statusClosedReturnsOnlyClosed() {
        saveOpenAndClosed();

        EventFilter filter = new EventFilter();
        filter.setStatus(StatusEnum.CLOSED);
        List<Event> events = find(filter, null);

        assertThat(events).extracting(Event::getEonetId).containsExactly("CLOSED_1");
    }

    @Test
    void startIsInclusive() {
        OffsetDateTime boundary = OffsetDateTime.parse("2026-01-01T00:00:00Z");

        eventRepository.save(EventTestData.anEvent().eonetId("ON_BOUNDARY")
                .eventDate(boundary).build());
        eventRepository.save(EventTestData.anEvent().eonetId("BEFORE")
                .eventDate(boundary.minusDays(1)).build());

        EventFilter filter = new EventFilter();
        filter.setStart(boundary);
        List<Event> events = find(filter, null);

        assertThat(events).extracting(Event::getEonetId).containsExactly("ON_BOUNDARY");
    }

    @Test
    void endIsInclusive() {
        OffsetDateTime boundary = OffsetDateTime.parse("2026-01-01T00:00:00Z");

        eventRepository.save(EventTestData.anEvent().eonetId("ON_BOUNDARY")
                .eventDate(boundary).build());
        eventRepository.save(EventTestData.anEvent().eonetId("AFTER")
                .eventDate(boundary.plusDays(1)).build());

        EventFilter filter = new EventFilter();
        filter.setEnd(boundary);
        List<Event> events = find(filter, null);

        assertThat(events).extracting(Event::getEonetId).containsExactly("ON_BOUNDARY");
    }

    @Test
    void sinceFiltersByIngestedAt() {

        eventRepository.save(EventTestData.anEvent().eonetId("E1").build());

        EventFilter futureFilter = new EventFilter();
        futureFilter.setSince(OffsetDateTime.parse("2099-01-01T00:00:00Z"));
        assertThat(find(futureFilter, null)).isEmpty();

        EventFilter pastFilter = new EventFilter();
        pastFilter.setSince(OffsetDateTime.parse("2000-01-01T00:00:00Z"));
        assertThat(find(pastFilter, null)).extracting(Event::getEonetId).containsExactly("E1");
    }

    @Test
    void bboxFiltersByCoordinates() {
        // Asymmetric box + one row outside on each single edge pins all four predicates
        // independently (removing any single lon/lat bound would leak its edge row).
        // INSIDE(8,3) is inside only under the correct lon/lat mapping, so a lon<->lat
        // axis swap in the Specification would wrongly exclude it and fail this test.
        eventRepository.save(EventTestData.anEvent().eonetId("INSIDE").geometry(8.0, 3.0).build());
        eventRepository.save(EventTestData.anEvent().eonetId("WEST").geometry(-20.0, 0.0).build());   // below minLon
        eventRepository.save(EventTestData.anEvent().eonetId("EAST").geometry(20.0, 0.0).build());    // above maxLon
        eventRepository.save(EventTestData.anEvent().eonetId("SOUTH").geometry(0.0, -20.0).build());  // below minLat
        eventRepository.save(EventTestData.anEvent().eonetId("NORTH").geometry(0.0, 20.0).build());   // above maxLat

        BoundingBox bbox = new BoundingBox(-10.0, -5.0, 10.0, 5.0);
        List<Event> events = find(new EventFilter(), bbox);

        assertThat(events).extracting(Event::getEonetId).containsExactly("INSIDE");
    }

    @Test
    void categoryJoinReturnsDistinctMatches() {
        eventRepository.save(EventTestData.anEvent().eonetId("MULTI")
                .category("wildfires", "floods").build());
        eventRepository.save(EventTestData.anEvent().eonetId("OTHER")
                .category("earthquakes").build());

        EventFilter filter = new EventFilter();
        filter.setCategory(List.of(CategoryEnum.WILDFIRES, CategoryEnum.FLOODS));
        List<Event> events = find(filter, null);

        assertThat(events).extracting(Event::getEonetId).containsExactly("MULTI");
    }
}

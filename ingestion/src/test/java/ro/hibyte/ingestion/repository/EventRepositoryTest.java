package ro.hibyte.ingestion.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import ro.hibyte.ingestion.support.AbstractPostgresIT;
import ro.hibyte.ingestion.support.EventTestData;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventRepositoryTest extends AbstractPostgresIT {

    @Autowired
    private EventRepository eventRepository;

    @Test
    void returnsDistinctCategoryIds() {
        eventRepository.save(EventTestData.anEvent().eonetId("E1").category("wildfires", "floods").build());
        eventRepository.save(EventTestData.anEvent().eonetId("E2").category("wildfires").build());

        assertThat(eventRepository.findDistinctCategoryIds())
                .containsExactlyInAnyOrder("wildfires", "floods");
    }
}

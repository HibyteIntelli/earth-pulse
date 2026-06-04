package ro.hibyte.ingestion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.hibyte.ingestion.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {
}

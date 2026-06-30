package ro.hibyte.ingestion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ro.hibyte.ingestion.model.Event;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, String>, JpaSpecificationExecutor<Event> {

    @Query("SELECT DISTINCT c FROM Event e JOIN e.categoryIds c")
    List<String> findDistinctCategoryIds();
}

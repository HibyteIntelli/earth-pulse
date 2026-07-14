package ro.hibyte.ingestion.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ro.hibyte.ingestion.dto.request.CategoryEnum;
import ro.hibyte.ingestion.dto.request.EventFilter;
import ro.hibyte.ingestion.dto.request.StatusEnum;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.model.EventStatus;
import ro.hibyte.ingestion.validation.BoundingBox;
import ro.hibyte.ingestion.validation.ValidatedEventFilter;

import java.util.ArrayList;
import java.util.List;

@Component
public class EventSpecification {

    public Specification<Event> build(ValidatedEventFilter validated) {
        EventFilter filter = validated.original();
        BoundingBox bbox = validated.bbox();

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            StatusEnum status = filter.getStatus() != null ? filter.getStatus() : StatusEnum.OPEN;

            if (status != StatusEnum.ALL) {
                EventStatus entityStatus = EventStatus.valueOf(status.name());
                predicates.add(cb.equal(root.get("status"), entityStatus));
            }

            if (filter.getStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), filter.getStart()));
            }

            if (filter.getEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), filter.getEnd()));
            }

            if (filter.getSince() != null) {
                predicates.add(cb.greaterThan(root.get("ingestedAt"), filter.getSince()));
            }

            if (bbox != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("longitude"), bbox.minLon()));
                predicates.add(cb.lessThanOrEqualTo(root.get("longitude"), bbox.maxLon()));
                predicates.add(cb.greaterThanOrEqualTo(root.get("latitude"), bbox.minLat()));
                predicates.add(cb.lessThanOrEqualTo(root.get("latitude"), bbox.maxLat()));
            }

            if (filter.getCategory() != null && !filter.getCategory().isEmpty()) {
                query.distinct(true);
                List<String> categoryIds = filter.getCategory().stream()
                        .map(CategoryEnum::getValue)
                        .toList();
                Join<Event, String> categoryJoin = root.join("categoryIds");
                predicates.add(categoryJoin.in(categoryIds));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

package ro.hibyte.ingestion.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ro.hibyte.ingestion.dto.request.CategoryEnum;
import ro.hibyte.ingestion.dto.request.EventFilter;
import ro.hibyte.ingestion.dto.request.StatusEnum;
import ro.hibyte.ingestion.exception.InvalidFilterException;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.model.EventStatus;

import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> build(EventFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            StatusEnum status = filter.getStatus() != null ? filter.getStatus() : StatusEnum.OPEN;

            if (status != StatusEnum.ALL) {
                EventStatus entityStatus = EventStatus.valueOf(status.name());
                predicates.add(cb.equal(root.get("status"), entityStatus));
            }

            if(filter.getStart() != null){
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), filter.getStart()));
            }

            if(filter.getEnd() != null){
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), filter.getEnd()));
            }

            if(filter.getSince() != null){
                predicates.add(cb.greaterThan(root.get("ingestedAt"), filter.getSince()));
            }

            if (filter.getBbox() != null) {
                String[] parts = filter.getBbox().split(",");
                if (parts.length != 4) {
                    throw new InvalidFilterException("invalid_bbox",
                            "bbox must have exactly 4 comma-separated values: min lon, max lat, max lon, min lat");
                }
                double min_lon, max_lat, max_lon, min_lat;
                try {
                    min_lon = Double.parseDouble(parts[0].trim());
                    max_lat = Double.parseDouble(parts[1].trim());
                    max_lon = Double.parseDouble(parts[2].trim());
                    min_lat = Double.parseDouble(parts[3].trim());
                } catch (NumberFormatException e) {
                    throw new InvalidFilterException("invalid_bbox", "bbox values must be valid numbers");
                }

                predicates.add(cb.greaterThanOrEqualTo(root.get("longitude"), min_lon));
                predicates.add(cb.lessThanOrEqualTo(root.get("longitude"), max_lon));
                predicates.add(cb.greaterThanOrEqualTo(root.get("latitude"), min_lat));
                predicates.add(cb.lessThanOrEqualTo(root.get("latitude"), max_lat));
            }

            if (filter.getCategory() != null && !filter.getCategory().isEmpty()) {
                query.distinct(true);
                List<String> values = filter.getCategory().stream()
                        .map(CategoryEnum::getValue)
                        .toList();
                Join<Event, String> categoryJoin = root.join("categoryIds");
                predicates.add(categoryJoin.in(values));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

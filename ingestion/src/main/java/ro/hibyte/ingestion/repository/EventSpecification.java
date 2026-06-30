package ro.hibyte.ingestion.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ro.hibyte.ingestion.dto.request.CategoryEnum;
import ro.hibyte.ingestion.dto.request.EventFilter;
import ro.hibyte.ingestion.dto.request.StatusEnum;
import ro.hibyte.ingestion.exception.InvalidFilterException;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.model.EventStatus;

import java.util.ArrayList;
import java.util.List;

@Component
public class EventSpecification {

    public Specification<Event> build(EventFilter filter) {
        double[] bbox = parseBbox(filter.getBbox());

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
                predicates.add(cb.greaterThanOrEqualTo(root.get("longitude"), bbox[0]));
                predicates.add(cb.lessThanOrEqualTo(root.get("longitude"), bbox[2]));
                predicates.add(cb.greaterThanOrEqualTo(root.get("latitude"), bbox[3]));
                predicates.add(cb.lessThanOrEqualTo(root.get("latitude"), bbox[1]));
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

    private double[] parseBbox(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split(",");
        if (parts.length != 4) {
            throw new InvalidFilterException("invalid_bbox",
                    "bbox must have exactly 4 comma-separated values: min lon, max lat, max lon, min lat");
        }

        double minLon, maxLat, maxLon, minLat;
        try {
            minLon = Double.parseDouble(parts[0].trim());
            maxLat = Double.parseDouble(parts[1].trim());
            maxLon = Double.parseDouble(parts[2].trim());
            minLat = Double.parseDouble(parts[3].trim());
        } catch (NumberFormatException e) {
            throw new InvalidFilterException("invalid_bbox", "bbox values must be valid numbers");
        }

        if (!Double.isFinite(minLon) || !Double.isFinite(maxLat)
                || !Double.isFinite(maxLon) || !Double.isFinite(minLat)) {
            throw new InvalidFilterException("invalid_bbox", "bbox values must be finite numbers");
        }

        if (minLon > maxLon || minLat > maxLat) {
            throw new InvalidFilterException("invalid_bbox", "bbox coordinates are invalid: min must not exceed max");
        }

        return new double[]{minLon, maxLat, maxLon, minLat};
    }
}

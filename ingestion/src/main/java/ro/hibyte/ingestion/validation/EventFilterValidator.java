package ro.hibyte.ingestion.validation;

import org.springframework.stereotype.Component;
import ro.hibyte.ingestion.dto.request.EventFilter;
import ro.hibyte.ingestion.exception.ErrorCode;
import ro.hibyte.ingestion.exception.InvalidFilterException;

@Component
public class EventFilterValidator {

    public ValidatedEventFilter validate(EventFilter filter) {
        if (filter == null) {
            filter = new EventFilter();
        }

        BoundingBox bbox = parseBbox(filter.getBbox());
        int size = validateSize(filter.getSize());
        int page = validatePage(filter.getPage());

        return new ValidatedEventFilter(filter, bbox, page, size);
    }

    private BoundingBox parseBbox(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split(",");
        if (parts.length != 4) {
            throw new InvalidFilterException(ErrorCode.INVALID_BBOX,
                    "bbox must have exactly 4 comma-separated values: min lon, max lat, max lon, min lat");
        }

        double minLon, maxLat, maxLon, minLat;
        try {
            minLon = Double.parseDouble(parts[0].trim());
            maxLat = Double.parseDouble(parts[1].trim());
            maxLon = Double.parseDouble(parts[2].trim());
            minLat = Double.parseDouble(parts[3].trim());
        } catch (NumberFormatException e) {
            throw new InvalidFilterException(ErrorCode.INVALID_BBOX, "bbox values must be valid numbers");
        }

        if (!Double.isFinite(minLon) || !Double.isFinite(maxLat)
                || !Double.isFinite(maxLon) || !Double.isFinite(minLat)) {
            throw new InvalidFilterException(ErrorCode.INVALID_BBOX, "bbox values must be finite numbers");
        }

        if (minLon > maxLon || minLat > maxLat) {
            throw new InvalidFilterException(ErrorCode.INVALID_BBOX, "bbox coordinates are invalid: min must not exceed max");
        }

        return new BoundingBox(minLon, minLat, maxLon, maxLat);
    }

    private int validateSize(Integer rawSize) {
        int size = rawSize != null ? rawSize : 100;
        if (size < 1 || size > 500) {
            throw new InvalidFilterException(ErrorCode.SIZE_OUT_OF_RANGE, "size must be between 1 and 500");
        }
        return size;
    }

    private int validatePage(Integer rawPage) {
        int page = rawPage != null ? rawPage : 0;
        if (page < 0) {
            throw new InvalidFilterException(ErrorCode.PAGE_OUT_OF_RANGE, "page must be >= 0");
        }
        return page;
    }
}

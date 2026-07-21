package ro.hibyte.ingestion.support;

import ro.hibyte.ingestion.dto.eonet.EonetCategory;
import ro.hibyte.ingestion.dto.eonet.EonetEvent;
import ro.hibyte.ingestion.dto.eonet.EonetGeometry;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.model.EventStatus;

import java.time.OffsetDateTime;
import java.util.*;

public final class EventTestData {

    private EventTestData() {
    }

    public static EventBuilder anEvent() {
        return new EventBuilder();
    }

    public static EonetEventBuilder anEonetEvent() {
        return new EonetEventBuilder();
    }

    public static final class EventBuilder {
        private String eonetId = "EONET_1";
        private String title = "Test Event";
        private EventStatus status = EventStatus.OPEN;
        private OffsetDateTime closedAt;
        private final Set<String> categoryIds = new HashSet<>();
        private Double longitude;
        private Double latitude;
        private OffsetDateTime eventDate;
        private Double magnitudeValue;
        private String magnitudeUnit;

        public EventBuilder eonetId(String eonetId) {
            this.eonetId = eonetId;
            return this;
        }

        public EventBuilder title(String title) {
            this.title = title;
            return this;
        }

        public EventBuilder open() {
            this.status = EventStatus.OPEN;
            this.closedAt = null;
            return this;
        }

        public EventBuilder closed(OffsetDateTime closedAt) {
            this.status = EventStatus.CLOSED;
            this.closedAt = closedAt;
            return this;
        }

        public EventBuilder category(String... ids) {
            this.categoryIds.addAll(Arrays.asList(ids));
            return this;
        }

        public EventBuilder geometry(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
            return this;
        }

        public EventBuilder eventDate(OffsetDateTime eventDate) {
            this.eventDate = eventDate;
            return this;
        }

        public EventBuilder magnitude(Double value, String unit) {
            this.magnitudeValue = value;
            this.magnitudeUnit = unit;
            return this;
        }

        public Event build() {
            Event event = new Event();
            event.setEonetId(eonetId);
            event.setTitle(title);
            event.setStatus(status);
            event.setClosedAt(closedAt);
            event.setCategoryIds(new HashSet<>(categoryIds));
            event.setLongitude(longitude);
            event.setLatitude(latitude);
            event.setEventDate(eventDate);
            event.setMagnitudeValue(magnitudeValue);
            event.setMagnitudeUnit(magnitudeUnit);
            return event;
        }
    }

    public static final class EonetEventBuilder {
        private String id = "EONET_1";
        private String title = "Test Event";
        private OffsetDateTime closed;
        private List<EonetCategory> categories;
        private List<EonetGeometry> geometry;

        public EonetEventBuilder id(String id) {
            this.id = id;
            return this;
        }

        public EonetEventBuilder title(String title) {
            this.title = title;
            return this;
        }

        public EonetEventBuilder closed(OffsetDateTime closed) {
            this.closed = closed;
            return this;
        }

        public EonetEventBuilder category(String... ids) {
            if (this.categories == null) {
                this.categories = new ArrayList<>();
            }
            for (String categoryId : ids) {
                EonetCategory category = new EonetCategory();
                category.setId(categoryId);
                this.categories.add(category);
            }
            return this;
        }

        public EonetEventBuilder geometry(double longitude, double latitude) {
            return geometryRaw(List.of(longitude, latitude));
        }

        public EonetEventBuilder geometry(double longitude, double latitude,
                                          OffsetDateTime date, Double magnitudeValue, String magnitudeUnit) {
            EonetGeometry g = new EonetGeometry();
            g.setCoordinates(List.of(longitude, latitude));
            g.setDate(date);
            g.setMagnitudeValue(magnitudeValue);
            g.setMagnitudeUnit(magnitudeUnit);
            return addGeometry(g);
        }

        public EonetEventBuilder geometryRaw(List<Object> coordinates) {
            EonetGeometry g = new EonetGeometry();
            g.setCoordinates(coordinates);
            return addGeometry(g);
        }

        private EonetEventBuilder addGeometry(EonetGeometry g) {
            if (this.geometry == null) {
                this.geometry = new ArrayList<>();
            }
            this.geometry.add(g);
            return this;
        }

        public EonetEvent build() {
            EonetEvent event = new EonetEvent();
            event.setId(id);
            event.setTitle(title);
            event.setClosed(closed);
            event.setCategories(categories);
            event.setGeometry(geometry);
            return event;
        }
    }
}

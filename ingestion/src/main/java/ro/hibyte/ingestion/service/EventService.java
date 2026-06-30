package ro.hibyte.ingestion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ro.hibyte.ingestion.client.EonetClient;
import ro.hibyte.ingestion.dto.eonet.EonetEvent;
import ro.hibyte.ingestion.dto.eonet.EonetResponse;
import ro.hibyte.ingestion.dto.request.EventFilter;
import ro.hibyte.ingestion.dto.request.SortEnum;
import ro.hibyte.ingestion.dto.response.EventPage;
import ro.hibyte.ingestion.dto.response.EventResponse;
import ro.hibyte.ingestion.exception.InvalidFilterException;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.repository.EventRepository;
import ro.hibyte.ingestion.repository.EventSpecification;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final EonetClient eonetClient;
    private final EventSpecification eventSpecification;

    private void upsertEvent(EonetEvent eonetEvent) {
        Event event = eventRepository.findById(eonetEvent.getId())
                .orElseGet(Event::new);
        event.setEonetId(eonetEvent.getId());
        event.applyFields(eonetEvent);
        eventRepository.save(event);
    }

    @Scheduled(fixedRateString = "${eonet.poll-interval-ms}")
    public void fetchAndSaveEvents() {
        try {
            EonetResponse response = eonetClient.fetchEvents();

            if (response == null || response.getEvents() == null) return;

            response.getEvents().forEach(event -> {
                try {
                    upsertEvent(event);
                } catch (Exception e) {
                    log.error("Failed to upsert event {}", event.getId(), e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to fetch events from EONET", e);
        }
    }

    public Optional<EventResponse> getEventById(String eonetId) {
        return eventRepository.findById(eonetId)
                .map(EventResponse::new);
    }

    public EventPage searchEvents(EventFilter filter) {
        if (filter == null) filter = new EventFilter();

        int size = filter.getSize() != null ? filter.getSize() : 100;
        int page = filter.getPage() != null ? filter.getPage() : 0;

        if (size < 1 || size > 500) {
            throw new InvalidFilterException("size_out_of_range", "size must be between 1 and 500");
        }
        if (page < 0) {
            throw new InvalidFilterException("page_out_of_range", "page must be >= 0");
        }

        Specification<Event> spec = eventSpecification.build(filter);
        Sort sort = buildSort(filter.getSort());

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Event> result = eventRepository.findAll(spec, pageable);

        List<EventResponse> items = result.getContent().stream()
                .map(EventResponse::new)
                .toList();

        return new EventPage(items, (int) result.getTotalElements(), page, size);
    }

    private Sort buildSort(SortEnum sortEnum) {
        SortEnum effective = sortEnum != null ? sortEnum : SortEnum.EVENT_DATE_DESC;
        String[] parts = effective.getValue().split(":");
        return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
    }
}

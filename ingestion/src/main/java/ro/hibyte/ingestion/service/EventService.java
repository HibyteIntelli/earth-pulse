package ro.hibyte.ingestion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private void upsertEvent(EonetEvent eonetEvent) {
        Event event = eventRepository.findById(eonetEvent.getId())
                .orElseGet(Event::new);
        event.setEonetId(eonetEvent.getId());
        event.applyFields(eonetEvent);
        eventRepository.save(event);
    }

    @Scheduled(fixedRateString = "${eonet.poll-interval-ms}")
    public void fetchAndSaveEvents(){
        try{
            EonetResponse response = eonetClient.fetchEvents();

            if (response == null || response.getEvents() == null) return;

            response.getEvents().forEach(event -> {
                try {
                    upsertEvent(event);
                } catch (Exception e) {
                    log.error("Failed to upsert event {}", event.getId(), e);
                }
            });
        } catch (Exception e){
            log.error("Failed to fetch events from EONET", e);
        }
    }

    public Optional<EventResponse> getEventById(String eonetId){
        return eventRepository.findById(eonetId)
                .map(EventResponse::new);
    }

    public EventPage searchEvents(EventFilter filter){
        if (filter == null) filter = new EventFilter();

        int limit = filter.getLimit() != null ? filter.getLimit() : 100;
        int offset = filter.getOffset() != null ? filter.getOffset() : 0;

        if (limit < 1 || limit > 500) {
            throw new InvalidFilterException("limit_out_of_range", "limit must be between 1 and 500");
        }
        if (offset < 0) {
            throw new InvalidFilterException("offset_out_of_range", "offset must be >= 0");
        }

        Specification<Event> spec = EventSpecification.build(filter);
        Sort sort = buildSort(filter.getSort());

        List<Event> all = eventRepository.findAll(spec, sort);

        List<EventResponse> items = all.stream()
                .skip(offset)
                .limit(limit)
                .map(EventResponse::new)
                .toList();

        return new EventPage(items, all.size(), limit, offset);
    }

    private Sort buildSort(SortEnum sortEnum) {
        SortEnum effective = sortEnum != null ? sortEnum : SortEnum.EVENT_DATE_DESC;
        String[] parts = effective.getValue().split(":");
        return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
    }
}

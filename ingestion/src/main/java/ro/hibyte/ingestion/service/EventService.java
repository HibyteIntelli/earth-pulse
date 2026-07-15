package ro.hibyte.ingestion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ro.hibyte.ingestion.client.EonetClient;
import ro.hibyte.ingestion.client.NotifierClient;
import ro.hibyte.ingestion.dto.eonet.EonetEvent;
import ro.hibyte.ingestion.dto.eonet.EonetResponse;
import ro.hibyte.ingestion.dto.notifier.NewEventPayloadDto;
import ro.hibyte.ingestion.dto.request.EventFilter;
import ro.hibyte.ingestion.dto.request.SortEnum;
import ro.hibyte.ingestion.dto.response.EventPage;
import ro.hibyte.ingestion.dto.response.EventResponse;
import ro.hibyte.ingestion.exception.EventAlreadyExistsException;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.repository.EventRepository;
import ro.hibyte.ingestion.repository.EventSpecification;
import ro.hibyte.ingestion.validation.EventFilterValidator;
import ro.hibyte.ingestion.validation.ValidatedEventFilter;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final EonetClient eonetClient;
    private final EventSpecification eventSpecification;
    private final NotifierClient notifierClient;
    private final EventFilterValidator eventFilterValidator;

    @Value("${eonet.poll-days:30}")
    private int pollDays;

    @Value("${eonet.backfill-days:30}")
    private int backfillDays;

    private void upsertEvent(EonetEvent eonetEvent, boolean notify) {
        try {
            saveEvent(eonetEvent, notify);
        } catch (DataIntegrityViolationException e) {
            if (eventRepository.existsById(eonetEvent.getId())) {
                log.debug("Concurrent insert for event {}, retrying as update", eonetEvent.getId());
                saveEvent(eonetEvent, notify);
            } else {
                throw e;
            }
        }
    }

    private void saveEvent(EonetEvent eonetEvent, boolean notify) {
        Optional<Event> existing = eventRepository.findById(eonetEvent.getId());
        boolean isNew = existing.isEmpty();
        Event event = existing.orElseGet(Event::new);
        event.setEonetId(eonetEvent.getId());
        event.applyFields(eonetEvent);
        Event saved = eventRepository.save(event);

        if (notify && isNew) {
            notifyNewEvent(saved);
        }
    }

    private void notifyNewEvent(Event event) {
        if (event.getLongitude() == null || event.getLatitude() == null || event.getEventDate() == null) {
            log.warn("Skipping notification for new event {}: missing geometry or event date", event.getEonetId());
            return;
        }
        notifierClient.notifyNewEvent(new NewEventPayloadDto(event));
    }

    public EventResponse ingestTestEvent(EonetEvent eonetEvent) {
        if (eventRepository.existsById(eonetEvent.getId())) {
            throw new EventAlreadyExistsException(
                    "Event with eonetId '" + eonetEvent.getId() + "' already exists");
        }
        upsertEvent(eonetEvent, true);
        return eventRepository.findById(eonetEvent.getId())
                .map(EventResponse::new)
                .orElseThrow();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillEvents() {
        if (eventRepository.count() == 0) {
            log.info("No events found, starting backfill process");
            fetchAndSave(backfillDays, false);
        }
    }

    @Scheduled(fixedRateString = "${eonet.poll-interval-ms:3600000}",
            initialDelayString = "${eonet.poll-initial-delay-ms:60000}")
    public void fetchAndSaveEvents() {
        fetchAndSave(pollDays, true);
    }

    private void fetchAndSave(int days, boolean notify) {
        try {
            EonetResponse response = eonetClient.fetchEvents(days);

            if (response == null || response.getEvents() == null) return;

            response.getEvents().forEach(event -> {
                try {
                    upsertEvent(event, notify);
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
        ValidatedEventFilter validated = eventFilterValidator.validate(filter);

        Specification<Event> spec = eventSpecification.build(validated);
        Sort sort = buildSort(validated.original().getSort());

        Pageable pageable = PageRequest.of(validated.page(), validated.size(), sort);
        Page<Event> result = eventRepository.findAll(spec, pageable);

        List<EventResponse> items = result.getContent().stream()
                .map(EventResponse::new)
                .toList();

        return new EventPage(items, (int) result.getTotalElements(), validated.page(), validated.size());
    }

    private Sort buildSort(SortEnum sortEnum) {
        SortEnum effective = sortEnum != null ? sortEnum : SortEnum.EVENT_DATE_DESC;
        String[] parts = effective.getValue().split(":");
        return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
    }
}

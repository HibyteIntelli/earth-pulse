package ro.hibyte.ingestion.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ro.hibyte.ingestion.client.EonetClient;
import ro.hibyte.ingestion.dto.EventResponse;
import ro.hibyte.ingestion.dto.eonet.EonetCategory;
import ro.hibyte.ingestion.dto.eonet.EonetEvent;
import ro.hibyte.ingestion.dto.eonet.EonetGeometry;
import ro.hibyte.ingestion.dto.eonet.EonetResponse;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.model.EventStatus;
import ro.hibyte.ingestion.repository.EventRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final EonetClient eonetClient;

    private Event mapToEntity(EonetEvent eonetEvent) {
        Event event = new Event();

        event.setEonetId(eonetEvent.getId());
        event.setTitle(eonetEvent.getTitle());
        event.setDescription(eonetEvent.getDescription());
        event.setLink(eonetEvent.getLink());

        event.setStatus(eonetEvent.getClosed() == null ? EventStatus.OPEN : EventStatus.CLOSED);
        event.setClosedAt(eonetEvent.getClosed());

        if (eonetEvent.getCategories() != null) {
            Set<String> categoryIds = eonetEvent.getCategories().stream()
                    .map(EonetCategory::getId)
                    .collect(Collectors.toSet());
            event.setCategoryIds(categoryIds);
        }

        List<EonetGeometry> geometries = eonetEvent.getGeometry();
        if (geometries != null && !geometries.isEmpty()) {
            EonetGeometry latest = geometries.get(geometries.size() - 1);
            event.setEventDate(latest.getDate());
            event.setMagnitudeValue(latest.getMagnitudeValue());
            event.setMagnitudeUnit(latest.getMagnitudeUnit());

            List<Object> coords = latest.getCoordinates();
            if (coords != null && coords.size() >= 2 && coords.get(0) instanceof Number) {
                event.setLongitude(((Number) coords.get(0)).doubleValue());
                event.setLatitude(((Number) coords.get(1)).doubleValue());
            }
        }
        return event;
    }

    private void upsertEvent(EonetEvent eonetEvent) {
        Optional<Event> existing = eventRepository.findById(eonetEvent.getId());

        if (existing.isPresent()) {
            Event event = existing.get();
            event.setStatus(eonetEvent.getClosed() == null ? EventStatus.OPEN : EventStatus.CLOSED);
            event.setClosedAt(eonetEvent.getClosed());
            eventRepository.save(event);
        } else {
            eventRepository.save(mapToEntity(eonetEvent));
        }
    }

    @Transactional
    @Scheduled(fixedRateString = "${eonet.poll-interval-ms}")
    public void fetchAndSaveEvents(){
        try{
            EonetResponse response = eonetClient.fetchEvents();

            if (response != null && response.getEvents() != null) {
                response.getEvents().forEach(this::upsertEvent);
            }
        } catch (Exception e){
            log.error("Failed to fetch events from EONET", e);
        }
    }

    private EventResponse mapToResponse(Event event){
        EventResponse eventResponse = new EventResponse();
        eventResponse.setEonetId(event.getEonetId());
        eventResponse.setTitle(event.getTitle());
        eventResponse.setDescription(event.getDescription());
        eventResponse.setLink(event.getLink());
        eventResponse.setStatus(event.getStatus());
        eventResponse.setClosedAt(event.getClosedAt());
        eventResponse.setCategoryIds(event.getCategoryIds());
        eventResponse.setEventDate(event.getEventDate());
        eventResponse.setLatitude(event.getLatitude());
        eventResponse.setLongitude(event.getLongitude());
        eventResponse.setMagnitudeValue(event.getMagnitudeValue());
        eventResponse.setMagnitudeUnit(event.getMagnitudeUnit());

        return eventResponse;
    }

    public List<EventResponse> getAllEvents(){
        return eventRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Optional<EventResponse> getEventById(String eonetId){
        return eventRepository.findById(eonetId)
                .map(this::mapToResponse);
    }
}

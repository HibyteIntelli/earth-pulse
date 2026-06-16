package ro.hibyte.ingestion.service;

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

import javax.xml.stream.EventFilter;
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

    private void applyFields(Event event, EonetEvent eonetEvent) {
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
    }

    private void upsertEvent(EonetEvent eonetEvent) {
        Event event = eventRepository.findById(eonetEvent.getId())
                .orElseGet(Event::new);
        event.setEonetId(eonetEvent.getId());
        applyFields(event, eonetEvent);
        eventRepository.save(event);
    }

    @Scheduled(fixedRateString = "${eonet.poll-interval-ms}")
    public void fetchAndSaveEvents(){
        try{
            EonetResponse response = eonetClient.fetchEvents();

            if (response != null && response.getEvents() != null) {
                response.getEvents().forEach(event -> {
                    try {
                        upsertEvent(event);
                    } catch (Exception e) {
                        log.error("Failed to upsert event {}", event.getId(), e);
                    }
                });
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

    public List<String> getCategories(){
        return eventRepository.findDistinctCategoryIds()
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<EventResponse> searchEvents(EventFilter filter){
        // TODO
        return null;
    }
}

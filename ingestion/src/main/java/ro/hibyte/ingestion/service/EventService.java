package ro.hibyte.ingestion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ro.hibyte.ingestion.client.EonetClient;
import ro.hibyte.ingestion.dto.eonet.EonetEvent;
import ro.hibyte.ingestion.dto.eonet.EonetResponse;
import ro.hibyte.ingestion.dto.request.EventFilter;
import ro.hibyte.ingestion.dto.response.EventPage;
import ro.hibyte.ingestion.dto.response.EventResponse;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.repository.EventRepository;

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
        // TODO
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

package ro.hibyte.ingestion.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.hibyte.ingestion.client.EonetClient;
import ro.hibyte.ingestion.client.NotifierClient;
import ro.hibyte.ingestion.dto.eonet.EonetCategory;
import ro.hibyte.ingestion.dto.eonet.EonetEvent;
import ro.hibyte.ingestion.dto.eonet.EonetGeometry;
import ro.hibyte.ingestion.dto.eonet.EonetResponse;
import ro.hibyte.ingestion.dto.notifier.NewEventPayloadDto;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.repository.EventRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final String EVENT_ID = "EONET_1";

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EonetClient eonetClient;

    @Mock
    private NotifierClient notifierClient;

    @InjectMocks
    private EventService eventService;

    private EonetEvent eonetEvent(boolean withGeometry) {
        EonetEvent event = new EonetEvent();
        event.setId(EVENT_ID);
        event.setTitle("Test event");

        EonetCategory category = new EonetCategory();
        category.setId("wildfires");
        category.setTitle("Wildfires");
        event.setCategories(List.of(category));

        if (withGeometry) {
            EonetGeometry geometry = new EonetGeometry();
            geometry.setType("Point");
            geometry.setDate(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
            geometry.setMagnitudeValue(5.0);
            geometry.setMagnitudeUnit("mag");
            geometry.setCoordinates(List.of(10.0, 20.0)); // [longitude, latitude]
            event.setGeometry(List.of(geometry));
        }

        return event;
    }

    private void stubFetch(EonetEvent event) {
        EonetResponse response = new EonetResponse();
        response.setEvents(List.of(event));
        when(eonetClient.fetchEvents(anyInt())).thenReturn(response);
    }

    @Test
    void notifiesNotifierWhenPollIngestsNewEvent() {
        stubFetch(eonetEvent(true));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        eventService.fetchAndSaveEvents();

        ArgumentCaptor<NewEventPayloadDto> captor = ArgumentCaptor.forClass(NewEventPayloadDto.class);
        verify(notifierClient, times(1)).notifyNewEvent(captor.capture());

        NewEventPayloadDto sent = captor.getValue();
        assertThat(sent.getEventId()).isEqualTo(EVENT_ID);
        assertThat(sent.getCategories()).containsExactly("wildfires");
        assertThat(sent.getGeometry().getCoordinates()).containsExactly(10.0, 20.0);
    }

    @Test
    void doesNotNotifyDuringBackfill() {
        stubFetch(eonetEvent(true));
        when(eventRepository.count()).thenReturn(0L);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        eventService.backfillEvents();

        verify(notifierClient, never()).notifyNewEvent(any());
    }

    @Test
    void doesNotNotifyWhenEventAlreadyExists() {
        stubFetch(eonetEvent(true));
        Event existing = new Event();
        existing.setEonetId(EVENT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));

        eventService.fetchAndSaveEvents();

        verify(notifierClient, never()).notifyNewEvent(any());
    }

    @Test
    void doesNotNotifyWhenNewEventHasNoGeometry() {
        stubFetch(eonetEvent(false));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        eventService.fetchAndSaveEvents();

        verify(notifierClient, never()).notifyNewEvent(any());
    }
}

package ro.hibyte.ingestion.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ro.hibyte.ingestion.client.EonetClient;
import ro.hibyte.ingestion.client.NotifierClient;
import ro.hibyte.ingestion.dto.eonet.EonetEvent;
import ro.hibyte.ingestion.dto.eonet.EonetResponse;
import ro.hibyte.ingestion.dto.notifier.NewEventPayloadDto;
import ro.hibyte.ingestion.dto.request.EventFilter;
import ro.hibyte.ingestion.dto.request.SortEnum;
import ro.hibyte.ingestion.dto.response.EventPage;
import ro.hibyte.ingestion.dto.response.EventResponse;
import ro.hibyte.ingestion.exception.EonetUnavailableException;
import ro.hibyte.ingestion.exception.EventAlreadyExistsException;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.repository.EventRepository;
import ro.hibyte.ingestion.repository.EventSpecification;
import ro.hibyte.ingestion.support.EventTestData;
import ro.hibyte.ingestion.validation.EventFilterValidator;
import ro.hibyte.ingestion.validation.ValidatedEventFilter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final String EVENT_ID = "EONET_1";

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EonetClient eonetClient;

    @Mock
    private NotifierClient notifierClient;

    @Mock
    private EventSpecification eventSpecification;

    @Mock
    private EventFilterValidator eventFilterValidator;

    @InjectMocks
    private EventService eventService;

    private EonetEvent eonetEvent(boolean withGeometry) {
        EventTestData.EonetEventBuilder builder = EventTestData.anEonetEvent().category("wildfires");
        if (withGeometry) {
            builder.geometry(10.0, 20.0, OffsetDateTime.parse("2026-01-01T00:00:00Z"), 5.0, "mag");
        }
        return builder.build();
    }

    private void stubFetch(EonetEvent event) {
        EonetResponse response = new EonetResponse();
        response.setEvents(List.of(event));
        when(eonetClient.fetchEvents(anyInt())).thenReturn(response);
    }

    @Nested
    class IngestTestEvent {

        @Test
        void returnsResponseForNewEvent() {
            when(eventRepository.existsById(EVENT_ID)).thenReturn(false);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            EventResponse response = eventService.ingestTestEvent(
                    EventTestData.anEonetEvent()
                            .geometry(10.0, 20.0, OffsetDateTime.parse("2026-01-01T00:00:00Z"), 5.0, "mag")
                            .build());

            assertThat(response.getId()).isEqualTo(EVENT_ID);
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        void throwsWhenAlreadyExists() {
            when(eventRepository.existsById(EVENT_ID)).thenReturn(true);

            assertThatThrownBy(() -> eventService.ingestTestEvent(EventTestData.anEonetEvent().build()))
                    .isInstanceOf(EventAlreadyExistsException.class);
            verify(eventRepository, never()).save(any());
        }

        @Test
        void throwsWhenRaceDuplicate() {
            when(eventRepository.existsById(EVENT_ID)).thenReturn(false, true);   // pre-check false, în catch true
            when(eventRepository.save(any(Event.class)))
                    .thenThrow(new DataIntegrityViolationException("dup"));

            assertThatThrownBy(() -> eventService.ingestTestEvent(EventTestData.anEonetEvent().build()))
                    .isInstanceOf(EventAlreadyExistsException.class);
        }

        @Test
        void rethrowsNonDuplicateDataIntegrityViolation() {
            when(eventRepository.existsById(EVENT_ID)).thenReturn(false, false);  // ambele false → nu e duplicat
            when(eventRepository.save(any(Event.class)))
                    .thenThrow(new DataIntegrityViolationException("other constraint"));

            assertThatThrownBy(() -> eventService.ingestTestEvent(EventTestData.anEonetEvent().build()))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    class PollAndBackfill {

        @Test
        void notifiesWhenPollIngestsNewEvent() {
            stubFetch(eonetEvent(true));
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            eventService.fetchAndSaveEvents();

            ArgumentCaptor<NewEventPayloadDto> captor = ArgumentCaptor.forClass(NewEventPayloadDto.class);
            verify(notifierClient, times(1)).notifyNewEvent(captor.capture());
            verify(eventRepository, times(1)).save(any(Event.class));

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

            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
            eventService.backfillEvents();
            verify(eventRepository, times(1)).save(any(Event.class));

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

        @Test
        void doesNotThrowWhenEonetFails() {
            when(eonetClient.fetchEvents(anyInt()))
                    .thenThrow(new EonetUnavailableException("down"));

            assertThatCode(() -> eventService.fetchAndSaveEvents()).doesNotThrowAnyException();
            verify(eventRepository, never()).save(any());
        }

        @Test
        void returnsCleanlyWhenResponseIsNull() {
            when(eonetClient.fetchEvents(anyInt())).thenReturn(null);

            assertThatCode(() -> eventService.fetchAndSaveEvents()).doesNotThrowAnyException();
            verify(eventRepository, never()).save(any());
        }

        @Test
        void continuesBatchWhenOneEventFails() {
            EonetResponse response = new EonetResponse();
            response.setEvents(List.of(
                    EventTestData.anEonetEvent().id("BAD").build(),
                    EventTestData.anEonetEvent().id("GOOD").build()));
            when(eonetClient.fetchEvents(anyInt())).thenReturn(response);
            when(eventRepository.findById(anyString())).thenReturn(Optional.empty());
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
                Event e = inv.getArgument(0);
                if ("BAD".equals(e.getEonetId())) {
                    throw new RuntimeException("boom");
                }
                return e;
            });

            eventService.fetchAndSaveEvents();

            verify(eventRepository).save(argThat(e -> "GOOD".equals(e.getEonetId())));
        }

        @Test
        void retriesAsUpdateOnConcurrentInsert() {
            stubFetch(eonetEvent(false));
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());
            when(eventRepository.save(any(Event.class)))
                    .thenThrow(new DataIntegrityViolationException("concurrent insert"))  // 1: coliziune
                    .thenAnswer(inv -> inv.getArgument(0));                               // 2: reușește
            when(eventRepository.existsById(EVENT_ID)).thenReturn(true);   // rândul există acum → e duplicat

            eventService.fetchAndSaveEvents();

            verify(eventRepository, times(2)).save(any(Event.class));   // s-a reîncercat ca update
        }

        @Test
        void doesNotRetryWhenDataIntegrityViolationIsNotDuplicate() {
            stubFetch(eonetEvent(false));
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());
            when(eventRepository.save(any(Event.class)))
                    .thenThrow(new DataIntegrityViolationException("other constraint"));
            when(eventRepository.existsById(EVENT_ID)).thenReturn(false);  // nu e duplicat

            eventService.fetchAndSaveEvents();

            verify(eventRepository, times(1)).save(any(Event.class));   // fără retry; rethrow înghițit de catch-ul batch
        }
    }

    @Nested
    class Search {

        private ValidatedEventFilter validated(EventFilter filter, int page, int size) {
            return new ValidatedEventFilter(filter, null, page, size);
        }

        private Pageable capturePageable() {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(eventRepository).findAll(any(Specification.class), captor.capture());
            return captor.getValue();
        }

        @Test
        void mapsContentAndCastsTotalToInt() {
            EventFilter filter = new EventFilter();
            when(eventFilterValidator.validate(any())).thenReturn(validated(filter, 0, 20));
            when(eventSpecification.build(any())).thenReturn((root, query, cb) -> null);
            Event event = EventTestData.anEvent().eonetId("E1").build();
            when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 42L));

            EventPage result = eventService.searchEvents(filter);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getId()).isEqualTo("E1");
            assertThat(result.getTotal()).isEqualTo(42);   // (int) cast din long
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(20);
        }

        @Test
        void defaultsToEventDateDescWhenSortIsNull() {
            EventFilter filter = new EventFilter();   // sort null
            when(eventFilterValidator.validate(any())).thenReturn(validated(filter, 0, 20));
            when(eventSpecification.build(any())).thenReturn((root, query, cb) -> null);
            when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            eventService.searchEvents(filter);

            Sort.Order order = capturePageable().getSort().getOrderFor("eventDate");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        void appliesFieldAndDirectionFromSortEnum() {
            EventFilter filter = new EventFilter();
            filter.setSort(SortEnum.INGESTED_AT_ASC);
            when(eventFilterValidator.validate(any())).thenReturn(validated(filter, 0, 20));
            when(eventSpecification.build(any())).thenReturn((root, query, cb) -> null);
            when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            eventService.searchEvents(filter);

            Sort.Order order = capturePageable().getSort().getOrderFor("ingestedAt");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
        }
    }
}

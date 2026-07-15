package ro.hibyte.ingestion.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ro.hibyte.ingestion.dto.eonet.EonetEvent;
import ro.hibyte.ingestion.dto.response.EventResponse;
import ro.hibyte.ingestion.exception.EventAlreadyExistsException;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.model.EventStatus;
import ro.hibyte.ingestion.service.EventService;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestEventController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TestEventControllerTest {

    private static final String VALID_BODY = """
            {
              "eonetId": "TEST-1",
              "title": "Test wildfire",
              "longitude": 26.1,
              "latitude": 44.4,
              "eventDate": "2026-07-15T10:00:00Z",
              "categoryIds": ["wildfires"],
              "magnitudeValue": 5.0,
              "magnitudeUnit": "MW"
            }
            """;

    private static final String BODY_MISSING_LONGITUDE = """
            {
              "eonetId": "TEST-1",
              "title": "Test wildfire",
              "latitude": 44.4,
              "eventDate": "2026-07-15T10:00:00Z"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private EventResponse sampleResponse() {
        Event event = new Event();
        event.setEonetId("TEST-1");
        event.setTitle("Test wildfire");
        event.setStatus(EventStatus.OPEN);
        event.setCategoryIds(Set.of("wildfires"));
        return new EventResponse(event);
    }

    @Test
    void returns202WhenEventIsIngested() throws Exception {
        when(eventService.ingestTestEvent(any(EonetEvent.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/internal/test/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("TEST-1"));
    }

    @Test
    void returns409WhenEventAlreadyExists() throws Exception {
        when(eventService.ingestTestEvent(any(EonetEvent.class)))
                .thenThrow(new EventAlreadyExistsException("Event with eonetId 'TEST-1' already exists"));

        mockMvc.perform(post("/internal/test/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("event_already_exists"));
    }

    @Test
    void returns400WhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(post("/internal/test/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_MISSING_LONGITUDE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("malformed_request"))
                .andExpect(jsonPath("$.message").value("longitude is required"));

        verify(eventService, never()).ingestTestEvent(any(EonetEvent.class));
    }
}

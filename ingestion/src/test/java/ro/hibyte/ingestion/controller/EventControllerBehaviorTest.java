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
import ro.hibyte.ingestion.dto.response.EventPage;
import ro.hibyte.ingestion.dto.response.EventResponse;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.model.EventStatus;
import ro.hibyte.ingestion.service.EventService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class EventControllerBehaviorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private EventResponse sampleResponse() {
        Event event = new Event();
        event.setEonetId("E1");
        event.setTitle("Some event");
        event.setStatus(EventStatus.OPEN);
        event.setCategoryIds(Set.of("wildfires"));
        return new EventResponse(event);
    }

    @Test
    void getByIdReturns200WhenPresent() throws Exception {
        when(eventService.getEventById("E1")).thenReturn(Optional.of(sampleResponse()));

        mockMvc.perform(get("/events/E1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("E1"));
    }

    @Test
    void getByIdReturns404WhenAbsent() throws Exception {
        when(eventService.getEventById("MISSING")).thenReturn(Optional.empty());

        mockMvc.perform(get("/events/MISSING"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchWithBodyReturns200() throws Exception {
        when(eventService.searchEvents(any()))
                .thenReturn(new EventPage(List.of(sampleResponse()), 1, 0, 20));

        mockMvc.perform(post("/events/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"open\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value("E1"));
    }

    @Test
    void searchWithNullBodyReturns200() throws Exception {
        when(eventService.searchEvents(any()))
                .thenReturn(new EventPage(List.of(), 0, 0, 20));

        mockMvc.perform(post("/events/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }
}

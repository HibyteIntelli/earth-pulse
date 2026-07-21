package ro.hibyte.ingestion.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.hibyte.ingestion.config.SecurityConfig;
import ro.hibyte.ingestion.dto.response.EventResponse;
import ro.hibyte.ingestion.model.Event;
import ro.hibyte.ingestion.model.EventStatus;
import ro.hibyte.ingestion.service.EventService;

import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalEventController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class InternalEventControllerSecurityTest {

    private static final String SECRET_HEADER = "X-Internal-Secret";
    private static final String VALID_SECRET = "test-internal-secret";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private EventResponse sampleResponse() {
        Event event = new Event();
        event.setEonetId("E1");
        event.setTitle("Some event");
        event.setStatus(EventStatus.OPEN);
        event.setCategoryIds(Set.of("wildfires"));
        return new EventResponse(event);
    }

    @Test
    void returns401WhenSecretMissing() throws Exception {
        mockMvc.perform(get("/internal/events/E1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void returns401WhenSecretWrong() throws Exception {
        mockMvc.perform(get("/internal/events/E1")
                        .header(SECRET_HEADER, "wrong-secret"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void returns200WhenSecretValid() throws Exception {
        when(eventService.getEventById("E1")).thenReturn(Optional.of(sampleResponse()));

        mockMvc.perform(get("/internal/events/E1")
                        .header(SECRET_HEADER, VALID_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("E1"));
    }
}

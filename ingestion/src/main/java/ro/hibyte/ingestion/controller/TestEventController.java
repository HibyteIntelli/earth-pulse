package ro.hibyte.ingestion.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.hibyte.ingestion.dto.request.TestEventRequest;
import ro.hibyte.ingestion.dto.response.EventResponse;
import ro.hibyte.ingestion.service.EventService;


@RestController
@RequestMapping("/internal/test/events")
@RequiredArgsConstructor
public class TestEventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> ingestTestEvent(@RequestBody TestEventRequest request) {
        EventResponse response = eventService.ingestTestEvent(request.toEonetEvent());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}

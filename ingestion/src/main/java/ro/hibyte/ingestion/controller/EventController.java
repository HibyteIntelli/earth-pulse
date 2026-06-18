package ro.hibyte.ingestion.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.hibyte.ingestion.dto.response.EventResponse;
import ro.hibyte.ingestion.service.EventService;
import ro.hibyte.ingestion.dto.request.EventFilter;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable String id) {
        return eventService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(eventService.getCategories());
    }

    @PostMapping("/search")
    public ResponseEntity<EventResponse> searchEvents(@RequestBody(required = false) EventFilter filter) {
        //TO DO
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}

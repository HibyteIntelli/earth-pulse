package ro.hibyte.notifier.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ro.hibyte.notifier.dto.NewEventPayloadDto;
import ro.hibyte.notifier.service.EventProcessingService;

@RestController
@RequiredArgsConstructor
public class InternalEventController {

    private final EventProcessingService eventProcessingService;

    @PostMapping("/internal/events/new")
    public ResponseEntity<Void> receiveNewEvent(@Valid @RequestBody NewEventPayloadDto payload) {
        eventProcessingService.processNewEvent(payload);
        return ResponseEntity.accepted().build();
    }
}

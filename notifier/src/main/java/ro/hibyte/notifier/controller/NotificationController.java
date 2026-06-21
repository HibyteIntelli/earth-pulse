package ro.hibyte.notifier.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ro.hibyte.notifier.dto.NotificationDto;
import ro.hibyte.notifier.dto.NotificationPageDto;
import ro.hibyte.notifier.entity.DeliveryMode;
import ro.hibyte.notifier.service.NotificationService;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public ResponseEntity<NotificationPageDto> listNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) DeliveryMode deliveryMode,
            @RequestParam(required = false) OffsetDateTime since,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {

        UUID userId = subjectAsUuid(jwt);
        return ResponseEntity.ok(
                notificationService.listNotifications(userId, eventId, category, deliveryMode, since, limit, offset));
    }

    @GetMapping("/notifications/{id}")
    public ResponseEntity<NotificationDto> getNotification(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = subjectAsUuid(jwt);
        return ResponseEntity.ok(notificationService.getNotification(id, userId));
    }

    private UUID subjectAsUuid(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token subject");
        }
    }
}

package ro.hibyte.notifier.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ro.hibyte.notifier.entity.CategoryEnum;
import ro.hibyte.notifier.dto.NotificationDto;
import ro.hibyte.notifier.dto.NotificationPageDto;
import ro.hibyte.notifier.entity.DeliveryMode;
import ro.hibyte.notifier.entity.NotificationLog;
import ro.hibyte.notifier.repository.NotificationLogRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationPageDto listNotifications(
            UUID userId,
            String eventId,
            CategoryEnum category,
            DeliveryMode deliveryMode,
            OffsetDateTime since,
            int limit,
            int offset) {

        PageRequest pageRequest = PageRequest.of(
                offset / limit, limit,
                Sort.by(Sort.Direction.DESC, "deliveredAt"));

        Page<NotificationLog> page = notificationLogRepository
                .findByFilters(
                        userId,
                        eventId,
                        category != null ? category.getValue() : null,
                        deliveryMode,
                        since,
                        pageRequest
                );

        return new NotificationPageDto(
                page.getContent().stream().map(NotificationDto::new).toList(),
                page.getTotalElements(),
                limit,
                offset
        );
    }

    public NotificationDto getNotification(UUID id, UUID userId) {
        NotificationLog log = notificationLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!log.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return new NotificationDto(log);
    }
}

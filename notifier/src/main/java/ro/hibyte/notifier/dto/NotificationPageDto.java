package ro.hibyte.notifier.dto;

import ro.hibyte.notifier.dto.NotificationDto;

import java.util.List;

public record NotificationPageDto(
        List<NotificationDto> items,
        long total,
        int limit,
        int offset
) {}
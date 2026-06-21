package ro.hibyte.notifier.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class NotificationPageDto {
    List<NotificationDto> items;
    long total;
    int limit;
    int offset;
}

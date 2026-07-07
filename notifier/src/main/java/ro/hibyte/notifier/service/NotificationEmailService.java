package ro.hibyte.notifier.service;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import ro.hibyte.notifier.dto.BriefingSnapshotDto;
import ro.hibyte.notifier.dto.MatchedWatchDto;
import ro.hibyte.notifier.dto.NewEventPayloadDto;
import ro.hibyte.notifier.entity.NotificationLog;
import ro.hibyte.notifier.entity.Severity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationEmailService {

    private static final DateTimeFormatter EVENT_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm 'UTC'");

    private static final Map<Severity, String> SEVERITY_COLORS = Map.of(
            Severity.LOW, "#16a34a",
            Severity.MODERATE, "#d97706",
            Severity.HIGH, "#dc2626",
            Severity.UNKNOWN, "#6b7280"
    );

    private static final Map<Severity, Integer> SEVERITY_RANK = Map.of(
            Severity.HIGH, 3,
            Severity.MODERATE, 2,
            Severity.LOW, 1,
            Severity.UNKNOWN, 0
    );

    private final SpringTemplateEngine templateEngine;
    private final EmailService emailService;

    public void sendImmediateEmail(MatchedWatchDto watch, NewEventPayloadDto payload, BriefingSnapshotDto briefing, String eventUrl) {
        Context context = new Context();
        context.setVariable("eventTitle", payload.getTitle());
        context.setVariable("eventDate", formatEventDate(payload.getEventDate()));
        context.setVariable("severity", briefing.getSeverity().name());
        context.setVariable("severityColor", SEVERITY_COLORS.get(briefing.getSeverity()));
        context.setVariable("summary", briefing.getSummary());
        context.setVariable("impact", briefing.getImpact());
        context.setVariable("precautions", briefing.getPrecautions());
        context.setVariable("eventUrl", eventUrl);

        String html = templateEngine.process("immediate-email", context);
        String subject = "[Earth Pulse] " + briefing.getSeverity().name() + " severity event: " + payload.getTitle();

        emailService.sendHtmlEmail(watch.getUserEmail(), subject, html);
    }

    public void sendDigestEmail(String userEmail, List<NotificationLog> events) {
        List<NotificationLog> sorted = events.stream()
                .sorted(Comparator
                        .comparing((NotificationLog n) -> SEVERITY_RANK.get(n.getBriefingSeverity()))
                        .reversed()
                        .thenComparing(NotificationLog::getEventDate, Comparator.reverseOrder()))
                .toList();

        Context context = new Context();
        context.setVariable("headerText", buildHeaderText(sorted));
        context.setVariable("events", sorted.stream().map(this::toEventView).toList());

        String html = templateEngine.process("digest-email", context);
        String subject = "[Earth Pulse] Daily digest: " + sorted.size() + " event" + (sorted.size() == 1 ? "" : "s") + " matched";

        emailService.sendHtmlEmail(userEmail, subject, html);
    }

    private String buildHeaderText(List<NotificationLog> events) {
        Map<Severity, Long> counts = events.stream()
                .collect(Collectors.groupingBy(NotificationLog::getBriefingSeverity, Collectors.counting()));

        StringBuilder text = new StringBuilder(events.size() + " event" + (events.size() == 1 ? "" : "s") + " matched: ")
                .append(counts.getOrDefault(Severity.HIGH, 0L)).append(" high, ")
                .append(counts.getOrDefault(Severity.MODERATE, 0L)).append(" moderate, ")
                .append(counts.getOrDefault(Severity.LOW, 0L)).append(" low");

        long unknown = counts.getOrDefault(Severity.UNKNOWN, 0L);
        if (unknown > 0) {
            text.append(", ").append(unknown).append(" unknown");
        }
        return text.toString();
    }

    private EventView toEventView(NotificationLog notificationLog) {
        return EventView.builder()
                .eventTitle(notificationLog.getEventTitle())
                .eventDate(formatEventDate(notificationLog.getEventDate()))
                .severity(notificationLog.getBriefingSeverity().name())
                .severityColor(SEVERITY_COLORS.get(notificationLog.getBriefingSeverity()))
                .summary(notificationLog.getBriefingSummary())
                .impact(notificationLog.getBriefingImpact())
                .precautions(notificationLog.getBriefingPrecautions())
                .eventUrl(notificationLog.getEventUrl())
                .build();
    }

    private String formatEventDate(OffsetDateTime eventDate) {
        return eventDate.withOffsetSameInstant(ZoneOffset.UTC).format(EVENT_DATE_FORMAT);
    }

    @Value
    @Builder
    private static class EventView {
        String eventTitle;
        String eventDate;
        String severity;
        String severityColor;
        String summary;
        String impact;
        List<String> precautions;
        String eventUrl;
    }
}

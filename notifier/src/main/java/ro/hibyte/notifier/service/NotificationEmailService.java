package ro.hibyte.notifier.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import ro.hibyte.notifier.dto.BriefingSnapshotDto;
import ro.hibyte.notifier.dto.MatchedWatchDto;
import ro.hibyte.notifier.dto.NewEventPayloadDto;
import ro.hibyte.notifier.entity.Severity;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

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

    private final SpringTemplateEngine templateEngine;
    private final EmailService emailService;

    public void sendImmediateEmail(MatchedWatchDto watch, NewEventPayloadDto payload, BriefingSnapshotDto briefing, String eventUrl) {
        Context context = new Context();
        context.setVariable("eventTitle", payload.getTitle());
        context.setVariable("eventDate", payload.getEventDate().withOffsetSameInstant(ZoneOffset.UTC).format(EVENT_DATE_FORMAT));
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
}

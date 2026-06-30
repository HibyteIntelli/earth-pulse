package com.api.llm.dto;

import com.api.llm.entity.Briefing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class BriefingResponseDto {
    private String eventId;
    private String readingLevel;
    private String summary;
    private String impact;
    private String severity;
    private List<String> precautions;
    private Instant generatedAt;

    public BriefingResponseDto(Briefing briefing) {
        this.eventId = briefing.getId().getEventId();
        this.readingLevel = briefing.getId().getReadingLevel();
        this.summary = briefing.getSummary();
        this.impact = briefing.getImpact();
        this.severity = briefing.getSeverity();
        this.precautions = briefing.getPrecautions();
        this.generatedAt = briefing.getGeneratedAt();
    }
}

package com.api.llm.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;

@Getter
@Setter
@Builder
public class BriefingResponseDto {
    private long eventId;
    private String readingLevel;
    private String summary;
    private String impact;
    private String severity;
    private ArrayList<String> precautions;
    private Instant generatedAt;
}

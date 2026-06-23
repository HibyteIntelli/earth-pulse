package com.api.llm.dto;

import lombok.Data;

@Data
public class BriefingRequestDto {
    private String eventId;
    private String readingLevel;
    private double magnitudeLevel;
    private String category;
}

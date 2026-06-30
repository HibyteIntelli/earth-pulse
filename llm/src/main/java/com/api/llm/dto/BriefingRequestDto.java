package com.api.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BriefingRequestDto {
    private String eventId;
    private String readingLevel;
    private double magnitudeLevel;
    private String category;
}

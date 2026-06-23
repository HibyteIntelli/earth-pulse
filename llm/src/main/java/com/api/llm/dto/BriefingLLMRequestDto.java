package com.api.llm.dto;

import lombok.Data;

@Data
public class BriefingLLMRequestDto {
    private String readingLevel;
    private double magnitudeLevel;
    private String category;
}

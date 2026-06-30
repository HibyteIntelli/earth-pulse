package com.api.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BriefingLLMRequestDto {
    private String readingLevel;
    private double magnitudeLevel;
    private String category;
}

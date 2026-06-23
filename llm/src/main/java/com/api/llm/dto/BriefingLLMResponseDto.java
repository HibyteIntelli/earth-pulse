package com.api.llm.dto;

import lombok.Data;

import java.util.List;

@Data
public class BriefingLLMResponseDto {
    private String summary;
    private String impact;
    private List<String> precautions;
}

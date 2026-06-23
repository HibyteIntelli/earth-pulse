package com.api.llm.dto;

import lombok.Data;

import java.util.ArrayList;

@Data
public class BriefingLLMResponseDto {
    private String summary;
    private String impact;
    private ArrayList<String> precautions;
}

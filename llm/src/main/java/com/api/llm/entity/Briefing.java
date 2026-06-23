package com.api.llm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Setter
public class Briefing {

    @EmbeddedId
    private BriefingId id;

    private String summary;
    private String severity;
    private String impact;
    private Instant generatedAt;

    @ElementCollection
    private List<String> precautions;

}

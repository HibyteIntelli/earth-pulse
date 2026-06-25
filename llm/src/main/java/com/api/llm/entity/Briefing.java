package com.api.llm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Briefing {

    @EmbeddedId
    private BriefingId id;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String severity;

    @Column(columnDefinition = "TEXT")
    private String impact;

    private Instant generatedAt;

    @ElementCollection
    private List<String> precautions;

}

package com.api.llm.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
public class BriefingId implements Serializable {
    private String eventId;

    private String readingLevel;

    public BriefingId() {

    }
}

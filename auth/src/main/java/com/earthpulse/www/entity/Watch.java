package com.earthpulse.www.entity;

import com.earthpulse.www.enums.ReadingLevel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "watches", indexes = @Index(columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class Watch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private String name;

    @Column(nullable = false)
    private double minLat;

    @Column(nullable = false)
    private double maxLat;

    @Column(nullable = false)
    private double minLon;

    @Column(nullable = false)
    private double maxLon;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "watch_categories", joinColumns = @JoinColumn(name = "watch_id"))
    @Column(name = "category")
    private List<String> categories = new ArrayList<>();

    @Column(nullable = false)
    private boolean digestMode = false;

    @Enumerated(EnumType.STRING)
    @Column
    private ReadingLevel readingLevel;

    @Column(nullable = false)
    private boolean active = true;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

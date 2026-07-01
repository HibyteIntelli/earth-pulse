package com.earthpulse.www.repository;

import com.earthpulse.www.entity.Watch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchRepository extends JpaRepository<Watch, UUID> {

    List<Watch> findAllByUserId(UUID userId);

    Optional<Watch> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT DISTINCT w FROM Watch w
            WHERE w.active = true
              AND w.minLat <= :lat AND w.maxLat >= :lat
              AND w.minLon <= :lon AND w.maxLon >= :lon
              AND (w.categories IS EMPTY OR :category MEMBER OF w.categories)
            """)
    List<Watch> findMatchingWatches(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("category") String category
    );
}

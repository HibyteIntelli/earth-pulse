package com.earthpulse.www.service;

import com.earthpulse.www.dto.EventQueryDto;
import com.earthpulse.www.dto.MatchingWatchDto;
import com.earthpulse.www.dto.WatchRequestDto;
import com.earthpulse.www.dto.WatchResponseDto;
import com.earthpulse.www.dto.WatchUpdateDto;
import com.earthpulse.www.exception.InvalidBoundingBoxException;
import com.earthpulse.www.exception.UserNotFoundException;
import com.earthpulse.www.exception.WatchNotFoundException;
import com.earthpulse.www.mapper.WatchMapper;
import com.earthpulse.www.repository.UserRepository;
import com.earthpulse.www.repository.WatchRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchService {

    private final WatchRepository watchRepository;
    private final UserRepository userRepository;
    private final WatchMapper watchMapper;

    private static final int MAX_WATCHES_PER_USER = 200;
    private static final int MAX_MATCHING_WATCHES = 1000;

    @Transactional(readOnly = true)
    public List<WatchResponseDto> list(UUID userId) {
        return watchRepository.findAllByUserId(userId, PageRequest.of(0, MAX_WATCHES_PER_USER))
                .stream()
                .map(watchMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public WatchResponseDto create(UUID userId, WatchRequestDto dto) {
        validateBoundingBox(dto.minLat(), dto.maxLat(), dto.minLon(), dto.maxLon());
        var userRef = userRepository.getReferenceById(userId);
        var watch = watchMapper.toEntity(dto, userRef);
        if (watch.getCategories() == null) {
            watch.setCategories(List.of());
        }
        try {
            return watchMapper.toResponseDto(watchRepository.save(watch));
        } catch (EntityNotFoundException e) {
            throw new UserNotFoundException(userId);
        }
    }

    @Transactional(readOnly = true)
    public WatchResponseDto get(UUID userId, UUID watchId) {
        return watchMapper.toResponseDto(
                watchRepository.findByIdAndUserId(watchId, userId)
                        .orElseThrow(() -> new WatchNotFoundException(watchId))
        );
    }

    @Transactional
    public WatchResponseDto update(UUID userId, UUID watchId, WatchUpdateDto dto) {
        var watch = watchRepository.findByIdAndUserId(watchId, userId)
                .orElseThrow(() -> new WatchNotFoundException(watchId));

        if (dto.name() != null) {
            watch.setName(dto.name().isBlank() ? null : dto.name());
        }

        if (dto.minLat() != null || dto.maxLat() != null || dto.minLon() != null || dto.maxLon() != null) {
            double newMinLat = dto.minLat() != null ? dto.minLat() : watch.getMinLat();
            double newMaxLat = dto.maxLat() != null ? dto.maxLat() : watch.getMaxLat();
            double newMinLon = dto.minLon() != null ? dto.minLon() : watch.getMinLon();
            double newMaxLon = dto.maxLon() != null ? dto.maxLon() : watch.getMaxLon();
            validateBoundingBox(newMinLat, newMaxLat, newMinLon, newMaxLon);
            watch.setMinLat(newMinLat);
            watch.setMaxLat(newMaxLat);
            watch.setMinLon(newMinLon);
            watch.setMaxLon(newMaxLon);
        }

        if (dto.categories() != null) {
            watch.setCategories(dto.categories());
        }

        if (dto.digestMode() != null) {
            watch.setDigestMode(dto.digestMode());
        }

        if (dto.readingLevel() != null) {
            watch.setReadingLevel(dto.readingLevel());
        }

        if (dto.active() != null) {
            watch.setActive(dto.active());
        }

        return watchMapper.toResponseDto(watchRepository.save(watch));
    }

    @Transactional
    public void delete(UUID userId, UUID watchId) {
        var watch = watchRepository.findByIdAndUserId(watchId, userId)
                .orElseThrow(() -> new WatchNotFoundException(watchId));
        watchRepository.delete(watch);
    }

    @Transactional(readOnly = true)
    public List<MatchingWatchDto> findMatching(EventQueryDto query) {
        return watchRepository.findMatchingWatches(query.lat(), query.lon(), query.category(),
                        PageRequest.of(0, MAX_MATCHING_WATCHES))
                .stream()
                .map(watchMapper::toMatchingDto)
                .toList();
    }

    private void validateBoundingBox(double minLat, double maxLat, double minLon, double maxLon) {
        if (minLat >= maxLat) {
            throw new InvalidBoundingBoxException("minLat must be less than maxLat");
        }
        if (minLon >= maxLon) {
            throw new InvalidBoundingBoxException("minLon must be less than maxLon");
        }
    }
}

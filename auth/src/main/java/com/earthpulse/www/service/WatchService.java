package com.earthpulse.www.service;

import com.earthpulse.www.dto.EventQueryDto;
import com.earthpulse.www.dto.MatchingWatchDto;
import com.earthpulse.www.dto.WatchRequestDto;
import com.earthpulse.www.dto.WatchResponseDto;
import com.earthpulse.www.dto.WatchUpdateDto;
import com.earthpulse.www.exception.UserNotFoundException;
import com.earthpulse.www.exception.WatchNotFoundException;
import com.earthpulse.www.mapper.WatchMapper;
import com.earthpulse.www.repository.UserRepository;
import com.earthpulse.www.repository.WatchRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional(readOnly = true)
    public List<WatchResponseDto> list(UUID userId) {
        return watchRepository.findAllByUserId(userId)
                .stream()
                .map(watchMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public WatchResponseDto create(UUID userId, WatchRequestDto dto) {
        validateBoundingBox(dto.minLat(), dto.maxLat(), dto.minLon(), dto.maxLon());
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        var watch = watchMapper.toEntity(dto, user);
        if (watch.getCategories() == null) {
            watch.setCategories(List.of());
        }
        return watchMapper.toResponseDto(watchRepository.save(watch));
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

        double newMinLat = dto.minLat() != null ? dto.minLat() : watch.getMinLat();
        double newMaxLat = dto.maxLat() != null ? dto.maxLat() : watch.getMaxLat();
        double newMinLon = dto.minLon() != null ? dto.minLon() : watch.getMinLon();
        double newMaxLon = dto.maxLon() != null ? dto.maxLon() : watch.getMaxLon();
        validateBoundingBox(newMinLat, newMaxLat, newMinLon, newMaxLon);
        watch.setMinLat(newMinLat);
        watch.setMaxLat(newMaxLat);
        watch.setMinLon(newMinLon);
        watch.setMaxLon(newMaxLon);

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
        return watchRepository.findMatchingWatches(query.lat(), query.lon(), query.category())
                .stream()
                .map(watchMapper::toMatchingDto)
                .toList();
    }

    private void validateBoundingBox(double minLat, double maxLat, double minLon, double maxLon) {
        if (minLat >= maxLat) {
            throw new IllegalArgumentException("minLat must be less than maxLat");
        }
        if (minLon >= maxLon) {
            throw new IllegalArgumentException("minLon must be less than maxLon");
        }
    }
}

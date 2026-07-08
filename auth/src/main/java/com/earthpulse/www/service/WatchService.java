package com.earthpulse.www.service;

import com.earthpulse.www.dto.EventQueryDto;
import com.earthpulse.www.dto.MatchingWatchDto;
import com.earthpulse.www.dto.WatchRequestDto;
import com.earthpulse.www.dto.WatchResponseDto;
import com.earthpulse.www.dto.WatchUpdateDto;
import com.earthpulse.www.enums.ReadingLevel;
import com.earthpulse.www.exception.DuplicateWatchNameException;
import com.earthpulse.www.exception.InvalidBoundingBoxException;
import com.earthpulse.www.exception.UserNotFoundException;
import com.earthpulse.www.exception.WatchLimitExceededException;
import com.earthpulse.www.exception.WatchNotFoundException;
import com.earthpulse.www.mapper.WatchMapper;
import com.earthpulse.www.repository.UserRepository;
import com.earthpulse.www.repository.WatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.watch.max-per-user:200}")
    private int maxWatchesPerUser;

    private static final int MAX_MATCHING_WATCHES = 1000;

    @Transactional(readOnly = true)
    public List<WatchResponseDto> list(UUID userId) {
        return watchRepository.findAllByUserId(userId, PageRequest.of(0, maxWatchesPerUser))
                .stream()
                .map(watchMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public WatchResponseDto create(UUID userId, WatchRequestDto dto) {
        validateBoundingBox(dto.minLat(), dto.maxLat(), dto.minLon(), dto.maxLon());
        var user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (watchRepository.countByUserId(userId) >= maxWatchesPerUser) {
            throw new WatchLimitExceededException(maxWatchesPerUser);
        }
        if (dto.name() != null && watchRepository.existsByUserIdAndName(userId, dto.name())) {
            throw new DuplicateWatchNameException(dto.name());
        }
        var watch = watchMapper.toEntity(dto, user);
        if (watch.getReadingLevel() == null) {
            watch.setReadingLevel(ReadingLevel.DEFAULT);
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
            String newName = dto.name().isBlank() ? null : dto.name();
            if (newName != null && !newName.equals(watch.getName())
                    && watchRepository.existsByUserIdAndName(userId, newName)) {
                throw new DuplicateWatchNameException(newName);
            }
            watch.setName(newName);
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

package com.earthpulse.www.mapper;

import com.earthpulse.www.dto.MatchingWatchDto;
import com.earthpulse.www.dto.WatchRequestDto;
import com.earthpulse.www.dto.WatchResponseDto;
import com.earthpulse.www.entity.User;
import com.earthpulse.www.entity.Watch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface WatchMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "name", source = "dto.name")
    @Mapping(target = "minLat", source = "dto.minLat")
    @Mapping(target = "maxLat", source = "dto.maxLat")
    @Mapping(target = "minLon", source = "dto.minLon")
    @Mapping(target = "maxLon", source = "dto.maxLon")
    @Mapping(target = "categories", source = "dto.categories")
    @Mapping(target = "digestMode", source = "dto.digestMode")
    @Mapping(target = "readingLevel", source = "dto.readingLevel")
    Watch toEntity(WatchRequestDto dto, User user);

    WatchResponseDto toResponseDto(Watch watch);

    @Mapping(target = "watchId", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    MatchingWatchDto toMatchingDto(Watch watch);
}

package com.earthpulse.www.mapper;

import com.earthpulse.www.dto.SignupRequestDto;
import com.earthpulse.www.dto.UserProfileDto;
import com.earthpulse.www.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "readingLevel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "profilePictureUrl", ignore = true)
    @Mapping(target = "email", source = "dto.email")
    @Mapping(target = "name", source = "dto.name")
    @Mapping(target = "passwordHash", source = "passwordHash")
    User toEntity(SignupRequestDto dto, String passwordHash);

    UserProfileDto toProfileDto(User user);
}

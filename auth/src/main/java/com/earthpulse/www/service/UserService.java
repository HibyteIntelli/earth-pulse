package com.earthpulse.www.service;

import com.earthpulse.www.dto.AuthResponseDto;
import com.earthpulse.www.dto.LoginRequestDto;
import com.earthpulse.www.dto.SignupRequestDto;
import com.earthpulse.www.dto.UpdateAccountRequestDto;
import com.earthpulse.www.dto.UserProfileDto;
import com.earthpulse.www.exception.BannedPasswordException;

import com.earthpulse.www.exception.DuplicateEmailException;
import com.earthpulse.www.exception.InvalidCredentialsException;
import com.earthpulse.www.exception.UserNotFoundException;
import com.earthpulse.www.exception.WrongPasswordException;
import com.earthpulse.www.mapper.UserMapper;
import com.earthpulse.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final BannedPasswordService bannedPasswordService;
    private final AvatarStorageService avatarStorage;

    @Transactional
    public void signup(SignupRequestDto dto) {
        if (bannedPasswordService.isBanned(dto.password())) {
            throw new BannedPasswordException();
        }
        if (userRepository.existsByEmail(dto.email())) {
            throw new DuplicateEmailException(dto.email());
        }
        try {
            userRepository.save(userMapper.toEntity(dto, passwordEncoder.encode(dto.password())));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException(dto.email());
        }
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto dto) {
        var user = userRepository.findByEmail(dto.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AuthResponseDto(jwtService.issueToken(user.getId()));
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return userMapper.toProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateAccount(UUID userId, UpdateAccountRequestDto dto) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (dto.email() != null) {
            if (dto.email().isBlank()) throw new IllegalArgumentException("Email cannot be blank");
            if (!dto.email().equals(user.getEmail()) && userRepository.existsByEmail(dto.email())) {
                throw new DuplicateEmailException(dto.email());
            }
            user.setEmail(dto.email());
        }

        if (dto.newPassword() != null && !dto.newPassword().isBlank()) {
            if (bannedPasswordService.isBanned(dto.newPassword())) {
                throw new BannedPasswordException();
            }
            if (dto.currentPassword() == null || !passwordEncoder.matches(dto.currentPassword(), user.getPasswordHash())) {
                throw new WrongPasswordException();
            }
            user.setPasswordHash(passwordEncoder.encode(dto.newPassword()));
        }

        boolean deleteAvatar = false;
        if (dto.profilePictureUrl() != null) {
            if (dto.profilePictureUrl().isBlank()) {
                user.setProfilePictureUrl(null);
                deleteAvatar = true;
            } else {
                user.setProfilePictureUrl(dto.profilePictureUrl());
            }
        }

        if (dto.readingLevel() != null) {
            user.setReadingLevel(dto.readingLevel());
        }

        if (dto.name() != null) {
            if (dto.name().isBlank()) throw new IllegalArgumentException("Name cannot be blank");
            user.setName(dto.name());
        }

        UserProfileDto result;
        try {
            result = userMapper.toProfileDto(userRepository.save(user));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException(dto.email());
        }

        if (deleteAvatar) {
            avatarStorage.delete(userId);
        }

        return result;
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.delete(user);
        avatarStorage.delete(userId);
    }
}

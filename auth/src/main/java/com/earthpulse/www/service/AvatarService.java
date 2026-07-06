package com.earthpulse.www.service;

import com.earthpulse.www.dto.UserProfileDto;
import com.earthpulse.www.exception.InvalidImageException;
import com.earthpulse.www.exception.UnsupportedImageTypeException;
import com.earthpulse.www.exception.UserNotFoundException;
import com.earthpulse.www.mapper.UserMapper;
import com.earthpulse.www.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    @Value("${app.avatar.max-file-size-bytes}")
    private long maxFileSizeBytes;

    private final AvatarStorageService avatarStorage;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserProfileDto upload(UUID userId, MultipartFile file) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (file.isEmpty()) {
            throw new InvalidImageException("Uploaded file is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new UnsupportedImageTypeException(contentType);
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new InvalidImageException("File exceeds the maximum allowed size of " + (maxFileSizeBytes / (1024 * 1024)) + " MB");
        }

        byte[] bytes;

        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidImageException("Unable to read uploaded image");
        }

        String ext = validateMagicBytes(bytes);
        String url = avatarStorage.store(userId, bytes, ext);

        user.setProfilePictureUrl(url);
        return userMapper.toProfileDto(userRepository.save(user));
    }

    private String validateMagicBytes(byte[] b) {
        if (b.length < 12) {
            throw new InvalidImageException("File too small to be a valid image");
        }
        if (isJpeg(b)) return "jpg";
        if (isPng(b)) return "png";
        if (isWebP(b)) return "webp";
        throw new InvalidImageException("File content does not match a supported image format (JPEG, PNG, or WebP)");
    }

    private boolean isJpeg(byte[] b) {
        return (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] b) {
        return (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    private boolean isWebP(byte[] b) {
        return b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }
}

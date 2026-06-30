package com.earthpulse.www.dto;

import com.earthpulse.www.enums.ReadingLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequestDto(

        @Email(message = "Invalid email format")
        String email,

        @Size(max = 128, message = "Password must not exceed 128 characters")
        String currentPassword,

        @Size(min = 8, max = 128, message = "New password must be between 8 and 128 characters")
        String newPassword,

        @Size(max = 2048, message = "Profile picture URL must not exceed 2048 characters")
        String profilePictureUrl,

        ReadingLevel readingLevel,

        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name
) {}

package com.earthpulse.www.dto;

import com.earthpulse.www.enums.ReadingLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequestDto(

        @Email(message = "Invalid email format")
        @Size(min = 1, message = "Email cannot be blank")
        String email,

        String currentPassword,

        @Size(min = 8, message = "New password must be at least 8 characters")
        String newPassword,

        String profilePictureUrl,

        ReadingLevel readingLevel,

        @Size(min = 1, message = "Name cannot be blank")
        String name
) {}

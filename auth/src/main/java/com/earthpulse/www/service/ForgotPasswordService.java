package com.earthpulse.www.service;

import com.earthpulse.www.dto.ForgotPasswordRequestDto;
import com.earthpulse.www.dto.ResetPasswordRequestDto;
import com.earthpulse.www.entity.PasswordResetToken;
import com.earthpulse.www.exception.BannedPasswordException;
import com.earthpulse.www.exception.InvalidResetTokenException;
import com.earthpulse.www.repository.PasswordResetTokenRepository;
import com.earthpulse.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final BannedPasswordService bannedPasswordService;
    private final EmailService emailService;

    @Value("${app.password-reset.token-expiry-minutes:60}")
    private int tokenExpiryMinutes;

    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto dto) {
        userRepository.findByEmail(dto.email()).ifPresent(user -> {
            tokenRepository.deleteByUser(user);
            String token = UUID.randomUUID().toString();
            Instant expiresAt = Instant.now().plus(tokenExpiryMinutes, ChronoUnit.MINUTES);
            tokenRepository.save(new PasswordResetToken(user, token, expiresAt));
            try {
                emailService.sendPasswordResetEmail(user.getEmail(), token);
            } catch (Exception e) {
                log.error("Failed to send password reset email to {}", user.getEmail(), e);
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto dto) {
        PasswordResetToken resetToken = tokenRepository.findByToken(dto.token())
                .orElseThrow(InvalidResetTokenException::new);

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidResetTokenException();
        }

        if (bannedPasswordService.isBanned(dto.newPassword())) {
            throw new BannedPasswordException();
        }

        resetToken.getUser().setPasswordHash(passwordEncoder.encode(dto.newPassword()));
        resetToken.setUsed(true);
    }
}

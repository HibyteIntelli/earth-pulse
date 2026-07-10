package com.earthpulse.www.service;

import com.earthpulse.www.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final PasswordResetTokenRepository tokenRepository;

    @Scheduled(cron = "${app.password-reset.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void deleteStaleTokens() {
        int deleted = tokenRepository.deleteStaleTokens(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} stale password reset token(s)", deleted);
        }
    }
}

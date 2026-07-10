package com.earthpulse.www.repository;

import com.earthpulse.www.entity.PasswordResetToken;
import com.earthpulse.www.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.used = true OR t.expiresAt < :now")
    int deleteStaleTokens(Instant now);
}

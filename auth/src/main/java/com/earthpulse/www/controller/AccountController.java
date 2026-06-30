package com.earthpulse.www.controller;

import com.earthpulse.www.dto.UpdateAccountRequestDto;
import com.earthpulse.www.dto.UserProfileDto;
import com.earthpulse.www.exception.InvalidCredentialsException;
import com.earthpulse.www.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(userService.getProfile(parseUserId(userId)));
    }

    @PatchMapping
    public ResponseEntity<UserProfileDto> updateAccount(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdateAccountRequestDto dto
    ) {
        return ResponseEntity.ok(userService.updateAccount(parseUserId(userId), dto));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal String userId) {
        userService.deleteAccount(parseUserId(userId));
        return ResponseEntity.noContent().build();
    }

    private UUID parseUserId(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException();
        }
    }
}

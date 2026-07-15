package com.earthpulse.www.controller;

import com.earthpulse.www.dto.AuthResponseDto;
import com.earthpulse.www.dto.ForgotPasswordRequestDto;
import com.earthpulse.www.dto.JwksDto;
import com.earthpulse.www.dto.LoginRequestDto;
import com.earthpulse.www.dto.MessageResponse;
import com.earthpulse.www.dto.ResetPasswordRequestDto;
import com.earthpulse.www.dto.SignupRequestDto;
import com.earthpulse.www.service.ForgotPasswordService;
import com.earthpulse.www.service.JwtService;
import com.earthpulse.www.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final ForgotPasswordService forgotPasswordService;

    @PostMapping(value = "/auth/signup", consumes = "application/json")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequestDto dto) {
        userService.signup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(value = "/auth/login", consumes = "application/json")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        return ResponseEntity.ok(userService.login(dto));
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<JwksDto> jwks() {
        return ResponseEntity.ok(jwtService.getJwks());
    }

    @PostMapping(value = "/auth/forgot-password", consumes = "application/json")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto dto) {
        forgotPasswordService.forgotPassword(dto);
        return ResponseEntity.ok(new MessageResponse("If that email is registered, a reset link has been sent."));
    }

    @PostMapping(value = "/auth/reset-password", consumes = "application/json")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequestDto dto) {
        forgotPasswordService.resetPassword(dto);
        return ResponseEntity.ok(new MessageResponse("Password reset successfully."));
    }
}

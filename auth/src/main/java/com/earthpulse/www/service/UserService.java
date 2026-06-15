package com.earthpulse.www.service;

import com.earthpulse.www.dto.AuthResponseDto;
import com.earthpulse.www.dto.LoginRequestDto;
import com.earthpulse.www.dto.SignupRequestDto;
import com.earthpulse.www.entity.User;
import com.earthpulse.www.exception.DuplicateEmailException;
import com.earthpulse.www.exception.InvalidCredentialsException;
import com.earthpulse.www.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public void signup(SignupRequestDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new DuplicateEmailException(dto.email());
        }
        var user = new User(dto.email(), passwordEncoder.encode(dto.password()));
        try {
            userRepository.save(user);
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
        try {
            return new AuthResponseDto(jwtService.issueToken(user.getId()));
        } catch (JOSEException e) {
            throw new RuntimeException("Token signing failed", e);
        }
    }
}

package com.earthpulse.www.service;

import com.earthpulse.www.dto.AuthResponseDto;
import com.earthpulse.www.dto.LoginRequestDto;
import com.earthpulse.www.dto.SignupRequestDto;
import com.earthpulse.www.entity.User;
import com.earthpulse.www.exception.DuplicateEmailException;
import com.earthpulse.www.exception.InvalidCredentialsException;
import com.earthpulse.www.mapper.UserMapper;
import com.earthpulse.www.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("signup: new email saves the user and returns without error")
    void signup_happyPath() {
        SignupRequestDto dto = new SignupRequestDto("alice@example.com", "password123", "Alice");
        User mappedUser = new User("alice@example.com", "hashed");

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(passwordEncoder.encode(dto.password())).thenReturn("hashed");
        when(userMapper.toEntity(dto, "hashed")).thenReturn(mappedUser);
        when(userRepository.save(mappedUser)).thenReturn(mappedUser);

        userService.signup(dto);
        verify(userRepository).save(mappedUser);
    }

    @Test
    @DisplayName("signup: duplicate email detected via existsByEmail throws DuplicateEmailException")
    void signup_duplicateEmail_existsCheck() {
        SignupRequestDto dto = new SignupRequestDto("bob@example.com", "password123", "Bob");

        when(userRepository.existsByEmail(dto.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.signup(dto))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("signup: passwordEncoder.encode is called with the raw (unhashed) password")
    void signup_passwordEncoder_calledWithRawPassword() {
        SignupRequestDto dto = new SignupRequestDto("alice@example.com", "rawPassword1", "Alice");
        User mappedUser = new User("alice@example.com", "hashed");

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(passwordEncoder.encode(dto.password())).thenReturn("hashed");
        when(userMapper.toEntity(dto, "hashed")).thenReturn(mappedUser);
        when(userRepository.save(mappedUser)).thenReturn(mappedUser);

        userService.signup(dto);

        verify(passwordEncoder).encode(eq("rawPassword1"));
    }

    @Test
    @DisplayName("signup: userMapper.toEntity receives the hashed password, not the raw password")
    void signup_hashedPassword_passedToMapper() {
        SignupRequestDto dto = new SignupRequestDto("alice@example.com", "rawPassword1", "Alice");
        User mappedUser = new User("alice@example.com", "hashed");

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(passwordEncoder.encode(dto.password())).thenReturn("hashed");
        when(userMapper.toEntity(dto, "hashed")).thenReturn(mappedUser);
        when(userRepository.save(mappedUser)).thenReturn(mappedUser);

        userService.signup(dto);

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(userMapper).toEntity(any(SignupRequestDto.class), passwordCaptor.capture());
        assertThat(passwordCaptor.getValue()).isEqualTo("hashed");
        assertThat(passwordCaptor.getValue()).isNotEqualTo("rawPassword1");
    }

    @Test
    @DisplayName("signup: DataIntegrityViolationException from repository is re-wrapped as DuplicateEmailException")
    void signup_duplicateEmail_raceCondition() {
        SignupRequestDto dto = new SignupRequestDto("carol@example.com", "password123", "Carol");
        User mappedUser = new User("carol@example.com", "hashed");

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(passwordEncoder.encode(dto.password())).thenReturn("hashed");
        when(userMapper.toEntity(dto, "hashed")).thenReturn(mappedUser);
        when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> userService.signup(dto))
                .isInstanceOf(DuplicateEmailException.class);
    }


    @Test
    @DisplayName("login: valid credentials return an AuthResponseDto containing a token")
    void login_happyPath() {
        LoginRequestDto dto = new LoginRequestDto("alice@example.com", "password123");
        UUID userId = UUID.randomUUID();
        User user = new User("alice@example.com", "hashed");
        user.setId(userId);

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), "hashed")).thenReturn(true);
        when(jwtService.issueToken(userId)).thenReturn("signed.jwt.token");

        AuthResponseDto response = userService.login(dto);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("signed.jwt.token");
    }

    @Test
    @DisplayName("login: unknown email throws InvalidCredentialsException")
    void login_unknownEmail() {
        LoginRequestDto dto = new LoginRequestDto("nobody@example.com", "password123");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).issueToken(any());
    }

    @Test
    @DisplayName("login: wrong password throws InvalidCredentialsException")
    void login_wrongPassword() {
        LoginRequestDto dto = new LoginRequestDto("alice@example.com", "wrongpassword");
        User user = new User("alice@example.com", "hashed");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).issueToken(any());
    }

    @Test
    @DisplayName("login: jwtService.issueToken is called with the authenticated user's UUID")
    void login_issueToken_calledWithCorrectUserId() {
        UUID userId = UUID.randomUUID();
        LoginRequestDto dto = new LoginRequestDto("alice@example.com", "password123");
        User user = new User("alice@example.com", "hashed");
        user.setId(userId);

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), "hashed")).thenReturn(true);
        when(jwtService.issueToken(userId)).thenReturn("signed.jwt.token");

        userService.login(dto);

        verify(jwtService).issueToken(eq(userId));
    }

    @Test
    @DisplayName("login: IllegalStateException from jwtService.issueToken propagates unwrapped")
    void login_issueToken_throwsIllegalStateException_propagates() {
        UUID userId = UUID.randomUUID();
        LoginRequestDto dto = new LoginRequestDto("alice@example.com", "password123");
        User user = new User("alice@example.com", "hashed");
        user.setId(userId);

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), "hashed")).thenReturn(true);
        when(jwtService.issueToken(userId)).thenThrow(new IllegalStateException("Token signing failed"));

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Token signing failed");
    }

    @Test
    @DisplayName("login: correct password but blank token string still returns the token from JwtService")
    void login_jwtServiceReturnValue_isPassedThrough() {
        LoginRequestDto dto = new LoginRequestDto("alice@example.com", "password123");
        UUID userId = UUID.randomUUID();
        User user = new User("alice@example.com", "hashed");
        user.setId(userId);

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.issueToken(userId)).thenReturn("another.jwt");

        AuthResponseDto response = userService.login(dto);

        assertThat(response.token()).isEqualTo("another.jwt");
    }
}

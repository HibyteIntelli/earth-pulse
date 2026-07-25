package com.earthpulse.www.service;

import com.earthpulse.www.dto.UpdateAccountRequestDto;
import com.earthpulse.www.dto.UserProfileDto;
import com.earthpulse.www.entity.User;
import com.earthpulse.www.enums.ReadingLevel;
import com.earthpulse.www.exception.BannedPasswordException;
import com.earthpulse.www.exception.DuplicateEmailException;
import com.earthpulse.www.exception.UserNotFoundException;
import com.earthpulse.www.exception.WrongPasswordException;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceAccountTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private BannedPasswordService bannedPasswordService;

    @InjectMocks
    private UserService userService;

    private final UUID USER_ID = UUID.randomUUID();

    // ---- getProfile ----

    @Test
    @DisplayName("getProfile: found user is mapped and returned")
    void getProfile_happyPath() {
        User user = makeUser();
        UserProfileDto dto = makeProfileDto(user);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toProfileDto(user)).thenReturn(dto);

        UserProfileDto result = userService.getProfile(USER_ID);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("getProfile: unknown userId throws UserNotFoundException")
    void getProfile_notFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ---- deleteAccount ----

    @Test
    @DisplayName("deleteAccount: existing user is deleted")
    void deleteAccount_happyPath() {
        User user = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.deleteAccount(USER_ID);

        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("deleteAccount: unknown userId throws UserNotFoundException and never deletes")
    void deleteAccount_notFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteAccount(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).delete(any());
    }

    // ---- updateAccount ----

    @Test
    @DisplayName("updateAccount: unknown userId throws UserNotFoundException")
    void updateAccount_notFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto(null, null, null, null, null, null)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("updateAccount: blank email throws IllegalArgumentException")
    void updateAccount_blankEmail_throws() {
        User user = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto("  ", null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("updateAccount: duplicate email (different from current) throws DuplicateEmailException")
    void updateAccount_duplicateEmail_throws() {
        User user = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto("taken@example.com", null, null, null, null, null)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("updateAccount: same email as current does not trigger duplicate check")
    void updateAccount_sameEmail_noConflict() {
        User user = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toProfileDto(user)).thenReturn(makeProfileDto(user));

        userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto(user.getEmail(), null, null, null, null, null));

        verify(userRepository, never()).existsByEmail(user.getEmail());
    }

    @Test
    @DisplayName("updateAccount: banned new password throws BannedPasswordException")
    void updateAccount_bannedPassword_throws() {
        User user = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(bannedPasswordService.isBanned("banned123")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto(null, "current", "banned123", null, null, null)))
                .isInstanceOf(BannedPasswordException.class);
    }

    @Test
    @DisplayName("updateAccount: missing currentPassword when changing password throws WrongPasswordException")
    void updateAccount_missingCurrentPassword_throws() {
        User user = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(bannedPasswordService.isBanned("newPass99")).thenReturn(false);

        assertThatThrownBy(() -> userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto(null, null, "newPass99", null, null, null)))
                .isInstanceOf(WrongPasswordException.class);
    }

    @Test
    @DisplayName("updateAccount: wrong currentPassword throws WrongPasswordException")
    void updateAccount_wrongCurrentPassword_throws() {
        User user = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(bannedPasswordService.isBanned("newPass99")).thenReturn(false);
        when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto(null, "wrong", "newPass99", null, null, null)))
                .isInstanceOf(WrongPasswordException.class);
    }

    @Test
    @DisplayName("updateAccount: correct currentPassword allows password change")
    void updateAccount_correctPassword_changesHash() {
        User user = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(bannedPasswordService.isBanned("newPass99")).thenReturn(false);
        when(passwordEncoder.matches("current", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newPass99")).thenReturn("newHash");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toProfileDto(user)).thenReturn(makeProfileDto(user));

        userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto(null, "current", "newPass99", null, null, null));

        assertThat(user.getPasswordHash()).isEqualTo("newHash");
    }

    @Test
    @DisplayName("updateAccount: blank profilePictureUrl is stored as null")
    void updateAccount_blankProfilePictureUrl_storedAsNull() {
        User user = makeUser();
        user.setProfilePictureUrl("http://old.example.com/pic.jpg");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toProfileDto(user)).thenReturn(makeProfileDto(user));

        userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto(null, null, null, "", null, null));

        assertThat(user.getProfilePictureUrl()).isNull();
    }

    @Test
    @DisplayName("updateAccount: non-blank profilePictureUrl replaces the existing URL")
    void updateAccount_nonBlankProfilePictureUrl_replaced() {
        User user = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toProfileDto(user)).thenReturn(makeProfileDto(user));

        userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto(null, null, null, "http://new.example.com/pic.jpg", null, null));

        assertThat(user.getProfilePictureUrl()).isEqualTo("http://new.example.com/pic.jpg");
    }

    @Test
    @DisplayName("updateAccount: readingLevel is updated when non-null")
    void updateAccount_readingLevelUpdated() {
        User user = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toProfileDto(user)).thenReturn(makeProfileDto(user));

        userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto(null, null, null, null, ReadingLevel.SIMPLIFIED, null));

        assertThat(user.getReadingLevel()).isEqualTo(ReadingLevel.SIMPLIFIED);
    }

    @Test
    @DisplayName("updateAccount: blank name throws IllegalArgumentException")
    void updateAccount_blankName_throws() {
        User user = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto(null, null, null, null, null, "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("updateAccount: non-blank name replaces the existing name")
    void updateAccount_nonBlankName_replaced() {
        User user = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toProfileDto(user)).thenReturn(makeProfileDto(user));

        userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto(null, null, null, null, null, "New Name"));

        assertThat(user.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("updateAccount: DataIntegrityViolationException from save is re-wrapped as DuplicateEmailException")
    void updateAccount_raceConditionOnEmail_throws() {
        User user = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("race@example.com")).thenReturn(false);
        when(userRepository.save(user)).thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> userService.updateAccount(USER_ID,
                new UpdateAccountRequestDto("race@example.com", null, null, null, null, null)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    // ---- helpers ----

    private User makeUser() {
        User u = new User("alice@example.com", "hashedPassword");
        u.setId(USER_ID);
        u.setName("Alice");
        u.setReadingLevel(ReadingLevel.DEFAULT);
        return u;
    }

    private UserProfileDto makeProfileDto(User user) {
        return new UserProfileDto(user.getId(), user.getEmail(), user.getName(),
                user.getReadingLevel(), user.getProfilePictureUrl(), Instant.now());
    }
}

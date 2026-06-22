package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.auth.AuthResponse;
import at.fhtechnikum.tourplanner.dto.auth.LoginRequest;
import at.fhtechnikum.tourplanner.dto.auth.RegisterRequest;
import at.fhtechnikum.tourplanner.dto.auth.UserResponse;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.model.UserSession;
import at.fhtechnikum.tourplanner.repository.AppUserRepository;
import at.fhtechnikum.tourplanner.repository.UserSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    AppUserRepository appUserRepository;
    @Mock
    UserSessionRepository userSessionRepository;

    @InjectMocks
    private AuthService authService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void register_createsUserAndSession_successfully() {
        RegisterRequest request = new RegisterRequest(
                "Example",
                "EXAMPLE@EXAMPLE.COM",
                "password123"
        );

        when(appUserRepository.existsByEmail("example@example.com")).thenReturn(false);
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse result = authService.register(request);

        assertThat(result.token()).isNotBlank();
        assertThat(result.user().name()).isEqualTo("Example");
        assertThat(result.user().email()).isEqualTo("example@example.com");

        verify(appUserRepository).existsByEmail("example@example.com");
        verify(appUserRepository).save(any(AppUser.class));
        verify(userSessionRepository).save(any(UserSession.class));
    }

    @Test
    void register_throwsConflict_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "Example",
                "example@example.com",
                "password123"
        );

        when(appUserRepository.existsByEmail("example@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("An account with this email already exists.");

        verify(appUserRepository).existsByEmail("example@example.com");
        verify(appUserRepository, never()).save(any(AppUser.class));
        verify(userSessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void login_returnsSession_whenPasswordMatches() {
        AppUser user = new AppUser();
        user.setName("Example");
        user.setEmail("example@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setCreatedAt(LocalDateTime.now());

        LoginRequest request = new LoginRequest(
                "example@example.com",
                "password123"
        );

        when(appUserRepository.findByEmail("example@example.com")).thenReturn(Optional.of(user));
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse result = authService.login(request);

        assertThat(result.token()).isNotBlank();
        assertThat(result.user().name()).isEqualTo("Example");
        assertThat(result.user().email()).isEqualTo("example@example.com");

        verify(appUserRepository).findByEmail("example@example.com");
        verify(userSessionRepository).save(any(UserSession.class));
    }

    @Test
    void login_returnsSession_whenPasswordMatchesAndCapslogEmail() {
        AppUser user = new AppUser();
        user.setName("Example");
        user.setEmail("example@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setCreatedAt(LocalDateTime.now());

        LoginRequest request = new LoginRequest(
                "EXAMPLE@EXAMPLE.COM",
                "password123"
        );

        when(appUserRepository.findByEmail("example@example.com")).thenReturn(Optional.of(user));
        //gibt das erste .save objekt zurück ohne eine DB zu bauen zum speichern
        //invocation ist zum simulierten Methodenaufruf, (0) ist für session rückgabewert
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse result = authService.login(request);

        assertThat(result.token()).isNotBlank();
        assertThat(result.user().name()).isEqualTo("Example");
        assertThat(result.user().email()).isEqualTo("example@example.com");

        verify(appUserRepository).findByEmail("example@example.com");
        verify(userSessionRepository).save(any(UserSession.class));
    }

    @Test
    void login_throwsUnauthorized_whenEmailDoesNotExist() {
        LoginRequest request = new LoginRequest(
                "missing@example.com",
                "password123"
        );

        when(appUserRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password.");

        verify(appUserRepository).findByEmail("missing@example.com");
        verify(userSessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void login_throwsUnauthorized_whenPasswordDoesNotMatch() {
        AppUser user = new AppUser();
        user.setName("Example");
        user.setEmail("example@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setCreatedAt(LocalDateTime.now());

        LoginRequest request = new LoginRequest(
                "example@example.com",
                "PASSWORD123"
        );

        when(appUserRepository.findByEmail("example@example.com")).thenReturn(Optional.of(user));
        //gibt das erste .save objekt zurück ohne eine DB zu bauen zum speichern
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password.");

        verify(appUserRepository).findByEmail("example@example.com");
        verify(userSessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void me_returnsUser_whenSessionIsValid() {
        AppUser user = new AppUser();
        user.setName("Example");
        user.setEmail("example@example.com");
        user.setCreatedAt(LocalDateTime.now());

        UserSession session = new UserSession();
        session.setToken("token-123");
        session.setUser(user);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(userSessionRepository.findById("token-123")).thenReturn(Optional.of(session));

        UserResponse result = authService.me("Bearer token-123");

        assertThat(result.name()).isEqualTo("Example");
        assertThat(result.email()).isEqualTo("example@example.com");
        verify(userSessionRepository).findById("token-123");
    }

    @Test
    void me_throwsUnauthorized_whenAuthorizationHeaderIsMissing() {
        assertThatThrownBy(() -> authService.me(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Missing session token.");

        verifyNoInteractions(userSessionRepository);
    }

    @Test
    void me_throwsUnauthorized_whenSessionDoesNotExist() {
        when(userSessionRepository.findById("token-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me("Bearer token-123"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid session.");

        verify(userSessionRepository).findById("token-123");
    }

    @Test
    void me_throwsUnauthorized_whenSessionIsExpired() {
        AppUser user = new AppUser();
        user.setName("Example");
        user.setEmail("example@example.com");
        user.setCreatedAt(LocalDateTime.now());

        UserSession session = new UserSession();
        session.setToken("token-123");
        session.setUser(user);
        session.setCreatedAt(LocalDateTime.now().minusDays(10));
        session.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(userSessionRepository.findById("token-123")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> authService.me("Bearer token-123"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Session expired.");

        verify(userSessionRepository).findById("token-123");
        verify(userSessionRepository).delete(session);
    }

    @Test
    void logout_deletesSession() {
        authService.logout("Bearer token-123");

        verify(userSessionRepository).deleteById("token-123");
    }
}

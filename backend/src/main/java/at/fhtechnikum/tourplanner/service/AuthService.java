package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.auth.AuthResponse;
import at.fhtechnikum.tourplanner.dto.auth.EditUserRequest;
import at.fhtechnikum.tourplanner.dto.auth.LoginRequest;
import at.fhtechnikum.tourplanner.dto.auth.RegisterRequest;
import at.fhtechnikum.tourplanner.dto.auth.UserResponse;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.model.UserSession;
import at.fhtechnikum.tourplanner.repository.AppUserRepository;
import at.fhtechnikum.tourplanner.repository.UserSessionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final int SESSION_DAYS = 7;
    private static final String DEMO_EMAIL = "demo@tourplanner.local";
    private static final String DEMO_PASSWORD = "demo1234";

    private final AppUserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AppUserRepository userRepository, UserSessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    @PostConstruct
    @Transactional
    public void seedDemoUser() {
        if (userRepository.existsByEmail(DEMO_EMAIL)) {
            return;
        }

        AppUser user = new AppUser();
        user.setName("Demo User");
        user.setEmail(DEMO_EMAIL);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String name = normalizeName(request.name());
        String email = normalizeEmail(request.email());
        String password = request.password().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());

        return createSession(userRepository.save(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        String password = request.password().trim();

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        return createSession(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(String authorizationHeader) {
        return toUserResponse(requireUser(authorizationHeader));
    }

    @Transactional
    public void logout(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        sessionRepository.deleteById(token);
    }

    @Transactional
    public UserResponse updateProfile(String authorizationHeader, EditUserRequest request) {
        AppUser user = requireUser(authorizationHeader);
        String name = normalizeName(request.name());
        String email = normalizeEmail(request.email());
        String currentPassword = request.currentPassword().trim();
        String newPassword = request.newPassword() == null ? "" : request.newPassword().trim();

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect.");
        }

        userRepository.findByEmail(email)
                .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                .ifPresent(existingUser -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Another account already uses this email address.");
                });

        user.setName(name);
        user.setEmail(email);

        if (!newPassword.isBlank()) {
            if (newPassword.length() < 6) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 6 characters.");
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        return toUserResponse(userRepository.save(user));
    }

    private AuthResponse createSession(AppUser user) {
        UserSession session = new UserSession();
        session.setToken(UUID.randomUUID().toString());
        session.setUser(user);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(SESSION_DAYS));
        sessionRepository.save(session);

        return new AuthResponse(session.getToken(), toUserResponse(user));
    }

    private AppUser requireUser(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        UserSession session = sessionRepository.findById(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session."));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            sessionRepository.delete(session);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired.");
        }

        return session.getUser();
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing session token.");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing session token.");
        }

        return token;
    }

    private UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        return name.trim();
    }
}

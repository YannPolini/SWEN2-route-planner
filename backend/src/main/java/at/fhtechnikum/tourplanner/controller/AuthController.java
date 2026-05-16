package at.fhtechnikum.tourplanner.controller;

import at.fhtechnikum.tourplanner.dto.auth.AuthResponse;
import at.fhtechnikum.tourplanner.dto.auth.EditUserRequest;
import at.fhtechnikum.tourplanner.dto.auth.LoginRequest;
import at.fhtechnikum.tourplanner.dto.auth.RegisterRequest;
import at.fhtechnikum.tourplanner.dto.auth.UserResponse;
import at.fhtechnikum.tourplanner.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(authService.me(authorizationHeader));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/editUser")
    public ResponseEntity<UserResponse> editUser(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody EditUserRequest request
    ) {
        return ResponseEntity.ok(authService.updateProfile(authorizationHeader, request));
    }
}

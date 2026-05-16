package at.fhtechnikum.tourplanner.dto.auth;

public record AuthResponse(
        String token,
        UserResponse user
) {
}

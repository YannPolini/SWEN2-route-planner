package at.fhtechnikum.tourplanner.dto.auth;

public record UserResponse(
        Long id,
        String name,
        String email
) {
}

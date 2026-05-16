package at.fhtechnikum.tourplanner.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 2) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password
) {
}

package at.fhtechnikum.tourplanner.dto.importexport;
import java.time.LocalDateTime;

public record ErrorResponseDto(
        LocalDateTime timestamp,
        int status,
        String error,
        String message
) {
}
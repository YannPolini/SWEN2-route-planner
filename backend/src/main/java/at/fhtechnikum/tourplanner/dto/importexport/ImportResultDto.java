package at.fhtechnikum.tourplanner.dto.importexport;

import java.util.List;

public record ImportResultDto(
        int importedRows,
        int failedRows,
        List<String> errors
) {
}
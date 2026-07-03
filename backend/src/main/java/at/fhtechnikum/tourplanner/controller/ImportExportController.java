package at.fhtechnikum.tourplanner.controller;

import at.fhtechnikum.tourplanner.dto.importexport.ImportResultDto;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.service.AuthService;
import at.fhtechnikum.tourplanner.service.ImportExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class ImportExportController {

    private final ImportExportService importExportService;
    private final AuthService authService;

    public ImportExportController(ImportExportService importExportService, AuthService authService) {
        this.importExportService = importExportService;
        this.authService = authService;
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultDto> importAll(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam("file") MultipartFile file
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        ImportResultDto result = importExportService.importTours(file, owner);
        return ResponseEntity.ok(result);
    }

    @GetMapping(path = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportAll(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam String format
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        byte[] csvFile = importExportService.exportAsCsv(owner);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvFile);
    }
}

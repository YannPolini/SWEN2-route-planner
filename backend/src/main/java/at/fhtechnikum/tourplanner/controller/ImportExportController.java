package at.fhtechnikum.tourplanner.controller;

import at.fhtechnikum.tourplanner.dto.importexport.ImportResultDto;
import at.fhtechnikum.tourplanner.service.ImportExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class ImportExportController {

    private final ImportExportService importExportService;

    public ImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    //Note: right now only as csv
    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultDto> exportAll(@RequestParam("file") MultipartFile file) {
        System.out.println("got file: " + file.getOriginalFilename());
        ImportResultDto result = importExportService.importTours(file);
        return ResponseEntity.ok(result);
    }

    /*  //not yet added
    @GetMapping("/api/tours/{tourId}/export")
    public ResponseEntity<String> exportTour(@PathVariable Long tourId) { return ResponseEntity.ok("export tour " + tourId); }
     */

    //Note: right now only as csv
    @GetMapping(path = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> importAll(@RequestParam String format) {
        byte[] csvFile = importExportService.exportAsCsv();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvFile);
    }

    /*  //not yet added, might add later
    @GetMapping("/api/tours/{tourId}/logs/export")
    public ResponseEntity<String> exportLogs(@PathVariable Long tourId) { return ResponseEntity.ok("export logs of tour " + tourId); }

    @GetMapping("/api/tours/{tourId}/logs/{logId}/export")
    public ResponseEntity<String> exportLog(@PathVariable Long tourId, @PathVariable Long logId) { return ResponseEntity.ok("export log " + logId); }
     */
}
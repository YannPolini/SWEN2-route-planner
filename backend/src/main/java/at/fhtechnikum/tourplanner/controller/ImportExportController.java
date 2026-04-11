@RestController
public class ImportExportController {

    @GetMapping("/api/tours/export")
    public ResponseEntity<String> exportAllTours() { return ResponseEntity.ok("export all tours"); }

    @GetMapping("/api/tours/{tourId}/export")
    public ResponseEntity<String> exportTour(@PathVariable Long tourId) { return ResponseEntity.ok("export tour " + tourId); }

    @PostMapping("/api/tours/import")
    public ResponseEntity<String> importTours() { return ResponseEntity.ok("import tours"); }

    @GetMapping("/api/tours/{tourId}/logs/export")
    public ResponseEntity<String> exportLogs(@PathVariable Long tourId) { return ResponseEntity.ok("export logs of tour " + tourId); }

    @GetMapping("/api/tours/{tourId}/logs/{logId}/export")
    public ResponseEntity<String> exportLog(@PathVariable Long tourId, @PathVariable Long logId) { return ResponseEntity.ok("export log " + logId); }
}
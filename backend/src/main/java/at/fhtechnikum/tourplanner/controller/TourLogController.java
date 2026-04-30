package at.fhtechnikum.tourplanner.controller;

import at.fhtechnikum.tourplanner.dto.tourlog.TourLog;
import at.fhtechnikum.tourplanner.service.TourLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/tours/{tourId}/logs")
@CrossOrigin(origins = "http://localhost:4200")
public class TourLogController {

    private final TourLogService service;

    public TourLogController(TourLogService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<TourLog> getAllTourLogs() {
        System.out.println("Getting all tour logs");
        return service.getAllTourLogs();
    }

    @GetMapping("/{logId}")
    public ResponseEntity<String> getOne(@PathVariable Long tourId, @PathVariable Long logId) { return ResponseEntity.ok("get log"); }

    @PostMapping
    public ResponseEntity<String> create(@PathVariable Long tourId) { return ResponseEntity.ok("create log"); }

    @PutMapping("/{logId}")
    public ResponseEntity<String> update(@PathVariable Long tourId, @PathVariable Long logId) { return ResponseEntity.ok("update log"); }

    @DeleteMapping("/{logId}")
    public ResponseEntity<String> delete(@PathVariable Long tourId, @PathVariable Long logId) { return ResponseEntity.ok("deleted"); }

    @GetMapping("/test")
    public String test() {
        return "controller works";
    }

}
package at.fhtechnikum.tourplanner.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/tours/{tourId}/logs")
public class TourLogController {

    @GetMapping
    public ResponseEntity<String> getAll(@PathVariable Long tourId) {
        System.out.println("here");
        return ResponseEntity.ok("get logs");
    }

    @GetMapping("/{logId}")
    public ResponseEntity<String> getOne(@PathVariable Long tourId, @PathVariable Long logId) { return ResponseEntity.ok("get log"); }

    @PostMapping
    public ResponseEntity<String> create(@PathVariable Long tourId) { return ResponseEntity.ok("create log"); }

    @PutMapping("/{logId}")
    public ResponseEntity<String> update(@PathVariable Long tourId, @PathVariable Long logId) { return ResponseEntity.ok("update log"); }

    @DeleteMapping("/{logId}")
    public ResponseEntity<String> delete(@PathVariable Long tourId, @PathVariable Long logId) { return ResponseEntity.ok("deleted"); }


}
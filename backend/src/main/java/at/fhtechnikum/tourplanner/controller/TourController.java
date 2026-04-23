package at.fhtechnikum.tourplanner.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tours")
public class TourController {

    @GetMapping
    public ResponseEntity<String> getAll() { return ResponseEntity.ok("get all tours"); }

    @GetMapping("/{tourId}")
    public ResponseEntity<String> getById(@PathVariable Long tourId) { return ResponseEntity.ok("get tour " + tourId); }

    @GetMapping("/category/{category}")
    public ResponseEntity<String> getByCategory(@PathVariable String category) { return ResponseEntity.ok("get by category"); }

    @PostMapping
    public ResponseEntity<String> create() { return ResponseEntity.ok("create tour"); }

    @PutMapping("/{tourId}")
    public ResponseEntity<String> update(@PathVariable Long tourId) { return ResponseEntity.ok("update tour"); }

    @DeleteMapping("/{tourId}")
    public ResponseEntity<String> delete(@PathVariable Long tourId) { return ResponseEntity.ok("deleted"); }

}
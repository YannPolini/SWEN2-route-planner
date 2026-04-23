package at.fhtechnikum.tourplanner.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/search")
public class SearchController{

    @GetMapping("/search")
    public ResponseEntity<String> searchGlobal(@RequestParam String query) { return ResponseEntity.ok("global search"); }

    @GetMapping("/tourlogs/search")
    public ResponseEntity<String> searchTourLogs(@RequestParam String query) { return ResponseEntity.ok("tourlog search"); }

    @GetMapping("/tours/search")
    public ResponseEntity<String> searchTours(@RequestParam String query) { return ResponseEntity.ok("tour search"); }

}
package at.fhtechnikum.tourplanner.controller;

import at.fhtechnikum.tourplanner.dto.search.SearchResultDto;
import at.fhtechnikum.tourplanner.dto.tour.Tour;
import at.fhtechnikum.tourplanner.dto.tourlog.TourLog;
import at.fhtechnikum.tourplanner.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "http://localhost:4200")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResultDto> searchGlobal(@RequestParam String query) {
        return ResponseEntity.ok(searchService.searchGlobal(query));
    }

    @GetMapping("/tours/search")
    public ResponseEntity<List<Tour>> searchTours(@RequestParam String query) {
        return ResponseEntity.ok(searchService.searchTours(query));
    }

    @GetMapping("/tourlogs/search")
    public ResponseEntity<List<TourLog>> searchTourLogs(@RequestParam String query) {
        return ResponseEntity.ok(searchService.searchTourLogs(query));
    }
}

package at.fhtechnikum.tourplanner.controller;

import at.fhtechnikum.tourplanner.dto.tour.AddressSuggestion;
import at.fhtechnikum.tourplanner.service.OrsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Proxies ORS geocoding so the API key stays on the server, never in the browser. */
@RestController
@RequestMapping("/api/geocode")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class GeocodeController {

    private final OrsService orsService;

    public GeocodeController(OrsService orsService) {
        this.orsService = orsService;
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<AddressSuggestion>> autocomplete(@RequestParam String text) {
        return ResponseEntity.ok(orsService.autocomplete(text));
    }
}

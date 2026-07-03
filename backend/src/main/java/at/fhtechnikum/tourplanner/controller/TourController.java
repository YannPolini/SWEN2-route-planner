package at.fhtechnikum.tourplanner.controller;

import at.fhtechnikum.tourplanner.dto.weather.WeatherForecastDto;
import at.fhtechnikum.tourplanner.exception.ResourceNotFoundException;
import at.fhtechnikum.tourplanner.model.Tour;
import at.fhtechnikum.tourplanner.service.TourService;
import at.fhtechnikum.tourplanner.service.WeatherService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tours")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class TourController {

    private final TourService service;
    private final WeatherService weatherService;

    public TourController(TourService service, WeatherService weatherService) {
        this.service = service;
        this.weatherService = weatherService;
    }

    @GetMapping("")
    public ResponseEntity<List<Tour>> getAll() {
        return ResponseEntity.ok(service.getAllTours());
    }

    @GetMapping("/{tourId}")
    public ResponseEntity<Tour> getById(@Valid @PathVariable String tourId) {
        return service.getTourById(tourId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{tourId}/weather")
    public ResponseEntity<WeatherForecastDto> getWeather(@Valid @PathVariable String tourId) {
        Tour tour = service.getTourById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: " + tourId));
        return ResponseEntity.ok(weatherService.getForecastForTour(tour));
    }

    @PostMapping
    public ResponseEntity<String> create(@Valid @RequestBody Tour dto) {
        service.createTour(dto);
        return ResponseEntity.ok("created tour");
    }

    @PutMapping("/{tourId}")
    public ResponseEntity<String> update(@Valid @PathVariable String tourId, @RequestBody Tour dto) {
        service.updateTour(tourId, dto);
        return ResponseEntity.ok("update tour");
    }

    @DeleteMapping("/{tourId}")
    public ResponseEntity<String> delete(@Valid @PathVariable String tourId) {
        service.deleteTour(tourId);
        return ResponseEntity.ok("deleted");
    }
}

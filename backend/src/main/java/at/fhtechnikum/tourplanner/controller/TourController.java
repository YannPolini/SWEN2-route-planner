package at.fhtechnikum.tourplanner.controller;

import at.fhtechnikum.tourplanner.dto.weather.WeatherForecastDto;
import at.fhtechnikum.tourplanner.exception.ResourceNotFoundException;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.model.Tour;
import at.fhtechnikum.tourplanner.service.AuthService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tours")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class TourController {

    private final TourService service;
    private final WeatherService weatherService;
    private final AuthService authService;

    public TourController(TourService service, WeatherService weatherService, AuthService authService) {
        this.service = service;
        this.weatherService = weatherService;
        this.authService = authService;
    }

    @GetMapping("")
    public ResponseEntity<List<Tour>> getAll(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        return ResponseEntity.ok(service.getToursForUser(owner));
    }

    @GetMapping("/{tourId}")
    public ResponseEntity<Tour> getById(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @PathVariable String tourId
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        return service.getTourById(tourId, owner)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{tourId}/weather")
    public ResponseEntity<WeatherForecastDto> getWeather(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @PathVariable String tourId
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        Tour tour = service.getTourById(tourId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: " + tourId));
        return ResponseEntity.ok(weatherService.getForecastForTour(tour));
    }

    @PostMapping
    public ResponseEntity<String> create(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody Tour dto
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        service.createTour(dto, owner);
        return ResponseEntity.ok("created tour");
    }

    @PutMapping("/{tourId}")
    public ResponseEntity<String> update(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @PathVariable String tourId,
            @Valid @RequestBody Tour dto
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        service.updateTour(tourId, dto, owner);
        return ResponseEntity.ok("update tour");
    }

    @DeleteMapping("")
    public ResponseEntity<String> deleteAll(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        int deletedCount = service.deleteToursForUser(owner);
        return ResponseEntity.ok("deleted " + deletedCount + " tours");
    }

    @DeleteMapping("/{tourId}")
    public ResponseEntity<String> delete(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @PathVariable String tourId
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        service.deleteTour(tourId, owner);
        return ResponseEntity.ok("deleted");
    }
}

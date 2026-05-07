package at.fhtechnikum.tourplanner.controller;

import at.fhtechnikum.tourplanner.dto.tour.Tour;
import at.fhtechnikum.tourplanner.dto.tourlog.TourLog;
import at.fhtechnikum.tourplanner.service.TourLogService;
import at.fhtechnikum.tourplanner.service.TourService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tours")
@CrossOrigin(origins = "http://localhost:4200")
public class TourController {

    private final TourService service;

    public TourController(TourService service) {
        this.service = service;
    }


    @GetMapping("")
    public ResponseEntity<List<Tour>> getAll() {
        System.out.println("getting all tours");
        return ResponseEntity.ok(service.getAllTours());
    }

    //not sure if works or even needed
    @GetMapping("/{tourId}")
    public ResponseEntity<Tour> getById(@PathVariable String tourId) {
        ResponseEntity.ok("get tour " + tourId);
        return service.getTourById(tourId)
                .map(ResponseEntity::ok)    //Wenn Update erfolgreich war, gib 200 OK mit dem aktualisierten Contact zurück.
                .orElse(ResponseEntity.notFound().build()); //oder das?
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<String> getByCategory(@PathVariable String category) { return ResponseEntity.ok("get by category"); }


    @PostMapping
    public ResponseEntity<String> create(@RequestBody Tour dto) {
        service.createTour(dto);
        return ResponseEntity.ok("created tour");
    }

    @PutMapping("/{tourId}")
    public ResponseEntity<String> update(@PathVariable String tourId, @RequestBody Tour dto) {
        System.out.println("trying update");
        service.updateTour(tourId, dto);
        return ResponseEntity.ok("update tour");
    }

    @DeleteMapping("/{tourId}")
    public ResponseEntity<String> delete(@PathVariable String tourId) {
        System.out.println("delete: " + tourId);
        service.deleteTour(tourId);
        return ResponseEntity.ok("deleted");
    }

}
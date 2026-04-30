package at.fhtechnikum.tourplanner.controller;

import at.fhtechnikum.tourplanner.dto.tourlog.TourLog;
import at.fhtechnikum.tourplanner.service.TourLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "http://localhost:4200")
public class TourLogController {

    private final TourLogService service;

    public TourLogController(TourLogService service) {
        this.service = service;
    }

    //funktioniert
    @GetMapping("")
    public List<TourLog> getAllTourLogs() {
        System.out.println("Getting all tour logs");
        return service.getAllTourLogs();
    }

    //brauch ich das überhaupt?
    @GetMapping("/{tourId}/{user}")
    public ResponseEntity<String> getOne(@PathVariable Long tourId, @PathVariable String user) {
        return ResponseEntity.ok("get log");
    }

    //funktioniert
    @PostMapping("")
    public ResponseEntity<String> create(@RequestBody TourLog dto) {
        System.out.println("Creating a new tour log");
        service.createTourLog(dto);
        return ResponseEntity.ok("create log");
    }


    @PutMapping("/{logId}")
    public ResponseEntity<String> update(@PathVariable Long logId, @RequestBody TourLog dto) {
        System.out.println("Updating a tour log");
        System.out.println("Saved log: " + dto.getComment());
        service.updateTourLog(logId, dto);
        return ResponseEntity.ok("update log");
    }

    @DeleteMapping("/{logId}")
    public ResponseEntity<String> delete(@PathVariable Long logId) {
        System.out.println("Deleting a tour log");
        service.deleteTourLog(logId);
        return ResponseEntity.ok("deleted");
    }

    @GetMapping("/test")
    public String test() {
        return "controller works";
    }

}
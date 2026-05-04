package at.fhtechnikum.tourplanner.controller;

import at.fhtechnikum.tourplanner.dto.tourlog.TourLog;
import at.fhtechnikum.tourplanner.service.TourLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.Map;


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
    public ResponseEntity<List<TourLog>> getAllTourLogs() {
        System.out.println("Getting all tour logs");
        //return service.getAllTourLogs();
        return ResponseEntity.ok( service.getAllTourLogs());
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
        //return ResponseEntity.ok("create log");
        return ResponseEntity.status(HttpStatus.CREATED).body("successfully created");  //das besser?
    }


    @PutMapping("/{logId}")
    public ResponseEntity<TourLog> update(@PathVariable Long logId, @RequestBody TourLog dto) {
        service.updateTourLog(logId, dto);
        //return ResponseEntity.ok("update log");
        return service.updateTourLog(logId, dto)
                .map(ResponseEntity::ok)    //Wenn Update erfolgreich war, gib 200 OK mit dem aktualisierten Contact zurück.
                .orElse(ResponseEntity.notFound().build()); //oder das?
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
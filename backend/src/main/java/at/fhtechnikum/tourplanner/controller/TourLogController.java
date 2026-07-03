package at.fhtechnikum.tourplanner.controller;

import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.model.TourLog;
import at.fhtechnikum.tourplanner.service.AuthService;
import at.fhtechnikum.tourplanner.service.TourLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/logs")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class TourLogController {

    private final TourLogService service;
    private final AuthService authService;

    public TourLogController(TourLogService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping("")
    public ResponseEntity<List<TourLog>> getAllTourLogs(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        return ResponseEntity.ok(service.getTourLogsForUser(owner));
    }

    @PostMapping("")
    public ResponseEntity<String> create(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody TourLog dto
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        service.createTourLog(dto, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body("successfully created");
    }

    @PutMapping("/{logId}")
    public ResponseEntity<TourLog> update(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @PathVariable String logId,
            @RequestBody TourLog dto
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        return service.updateTourLog(logId, dto, owner)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{logId}")
    public ResponseEntity<String> delete(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @PathVariable String logId
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        service.deleteTourLog(logId, owner);
        return ResponseEntity.ok("deleted");
    }

    @GetMapping("/test")
    public String test() {
        return "controller works";
    }
}

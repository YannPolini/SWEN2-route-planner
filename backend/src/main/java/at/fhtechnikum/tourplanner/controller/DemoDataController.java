package at.fhtechnikum.tourplanner.controller;

import at.fhtechnikum.tourplanner.dto.demo.DemoSeedResponse;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.service.AuthService;
import at.fhtechnikum.tourplanner.service.DemoDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo-data")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class DemoDataController {

    private final DemoDataService demoDataService;
    private final AuthService authService;

    public DemoDataController(DemoDataService demoDataService, AuthService authService) {
        this.demoDataService = demoDataService;
        this.authService = authService;
    }

    @PostMapping("/seed")
    public ResponseEntity<DemoSeedResponse> seedDemoData(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        AppUser owner = authService.requireUser(authorizationHeader);
        return ResponseEntity.ok(demoDataService.seedDemoData(owner));
    }
}

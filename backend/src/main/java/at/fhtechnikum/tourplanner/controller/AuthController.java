package at.fhtechnikum.tourplanner.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<String> register() { return ResponseEntity.ok("register"); }

    @PostMapping("/login")
    public ResponseEntity<String> login() { return ResponseEntity.ok("login"); }

    @GetMapping("/me")
    public ResponseEntity<String> me() { return ResponseEntity.ok("me"); }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() { return ResponseEntity.ok("logout"); }

    @PutMapping("/editUser")
    public ResponseEntity<String> editUser() { return ResponseEntity.ok("editUser"); }
}
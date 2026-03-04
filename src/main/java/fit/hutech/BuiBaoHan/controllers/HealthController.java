package fit.hutech.BuiBaoHan.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple health check endpoint for Railway deployment.
 * This endpoint does NOT depend on database or any external service.
 * It returns 200 OK as soon as the Spring Boot web server is ready.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}

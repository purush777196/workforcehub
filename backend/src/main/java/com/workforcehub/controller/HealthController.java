package com.workforcehub.controller;

import com.workforcehub.dto.ApiResponse;
import com.workforcehub.dto.DatabaseHealthResponse;
import com.workforcehub.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health and diagnostics REST controller.
 * Provides application heartbeat and database verification endpoints.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;

    @Autowired
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * Confirms that the Spring Boot backend server is active and responding.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApplicationHealth() {
        Map<String, Object> health = healthService.getApplicationHealth();
        return ResponseEntity.ok(ApiResponse.ok("WorkforceHub API is operational", health));
    }

    /**
     * Tests PostgreSQL database connectivity without leaking internal topology or credentials.
     */
    @GetMapping("/database")
    public ResponseEntity<ApiResponse<DatabaseHealthResponse>> getDatabaseHealth() {
        DatabaseHealthResponse dbHealth = healthService.checkDatabaseHealth();
        if (dbHealth.isConnected()) {
            return ResponseEntity.ok(ApiResponse.ok("Database connectivity verified", dbHealth));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiResponse<>(false, "Database connectivity probe failed", dbHealth));
        }
    }
}

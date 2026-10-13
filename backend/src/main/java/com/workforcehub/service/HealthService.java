package com.workforcehub.service;

import com.workforcehub.dto.DatabaseHealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostic service providing application and database health checks.
 */
@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);
    private static final Instant APP_START_TIME = Instant.now();

    private final DataSource dataSource;

    @Autowired
    public HealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Checks core application status.
     */
    public Map<String, Object> getApplicationHealth() {
        Map<String, Object> details = new HashMap<>();
        details.put("status", "UP");
        details.put("application", "WorkforceHub Backend");
        details.put("version", "0.0.1-SNAPSHOT");
        details.put("startedAt", APP_START_TIME);
        details.put("currentTime", Instant.now());
        return details;
    }

    /**
     * Performs a fast non-blocking probe against the configured PostgreSQL datasource.
     * Sanitizes errors to prevent exposing connection URLs, credentials, or internal topology.
     */
    public DatabaseHealthResponse checkDatabaseHealth() {
        long startTime = System.currentTimeMillis();

        if (dataSource == null) {
            long elapsed = System.currentTimeMillis() - startTime;
            return DatabaseHealthResponse.down(elapsed, "No configured datasource available in context.");
        }

        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2); // 2-second timeout probe
            if (!valid) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SELECT 1;");
                }
            }
            long elapsed = System.currentTimeMillis() - startTime;
            return DatabaseHealthResponse.up(elapsed);
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Database health check probe failed ({} ms): {}", elapsed, ex.getMessage());
            
            String sanitizedReason = "Database connection unavailable. Verify host, port, credentials, and service state.";
            return DatabaseHealthResponse.down(elapsed, sanitizedReason);
        }
    }
}

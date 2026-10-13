package com.workforcehub.dto;

import java.time.Instant;

/**
 * Diagnostic payload reporting PostgreSQL connectivity status.
 * Strictly sanitizes internal hostnames, credentials, and connection strings.
 */
public class DatabaseHealthResponse {

    private String status;
    private String database;
    private boolean connected;
    private long responseTimeMs;
    private String message;
    private Instant timestamp;

    public DatabaseHealthResponse() {
        this.timestamp = Instant.now();
    }

    public DatabaseHealthResponse(String status, String database, boolean connected, long responseTimeMs, String message) {
        this.status = status;
        this.database = database;
        this.connected = connected;
        this.responseTimeMs = responseTimeMs;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public static DatabaseHealthResponse up(long responseTimeMs) {
        return new DatabaseHealthResponse(
            "UP",
            "PostgreSQL",
            true,
            responseTimeMs,
            "Database connection established and healthy."
        );
    }

    public static DatabaseHealthResponse down(long responseTimeMs, String sanitizedReason) {
        return new DatabaseHealthResponse(
            "DOWN",
            "PostgreSQL",
            false,
            responseTimeMs,
            sanitizedReason != null ? sanitizedReason : "Unable to reach configured database."
        );
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

package com.workforcehub.controller;

import com.workforcehub.dto.ApiResponse;
import com.workforcehub.dto.importing.*;
import com.workforcehub.service.ImportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Administrative controller providing secure, audited data import and migration utilities.
 * Strictly restricted to users with ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/import")
@PreAuthorize("hasRole('ADMIN')")
public class ImportController {

    private final ImportService importService;

    @Autowired
    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    /**
     * Non-destructive preview and validation of incoming source records.
     * Evaluates dependencies, duplicate detection, and reference integrity without mutating PostgreSQL.
     */
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<ImportPreviewResponseDto>> previewImport(
            @RequestBody ImportPayloadDto payload,
            Authentication authentication
    ) {
        ImportPreviewResponseDto preview = importService.previewImport(payload, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Data import preview generated successfully.", preview));
    }

    /**
     * Executes data import into PostgreSQL within transactional boundaries.
     * Requires explicit confirmation that a database backup has been taken.
     */
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<ImportResultDto>> executeImport(
            @Valid @RequestBody ImportExecuteRequestDto request,
            Authentication authentication
    ) {
        ImportResultDto result = importService.executeImport(request, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Data import executed successfully.", result));
    }

    /**
     * Retrieves status and audit trail for a specific historical import job.
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<ImportJobDto>> getJobStatus(
            @PathVariable UUID jobId
    ) {
        ImportJobDto job = importService.getJobStatus(jobId);
        return ResponseEntity.ok(ApiResponse.ok("Import job retrieved successfully.", job));
    }
}

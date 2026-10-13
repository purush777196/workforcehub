package com.workforcehub.controller;

import com.workforcehub.dto.*;
import com.workforcehub.entity.enums.AttendanceStatus;
import com.workforcehub.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller managing daily attendance check-ins, check-outs,
 * administrative overrides, and summary metrics.
 */
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Autowired
    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * Retrieves attendance records with pagination, search, and filtering.
     * Accessible by ADMIN (all records) and EMPLOYEE (own records only).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAttendance(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date-desc") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "false") boolean all,
            Authentication authentication
    ) {
        AttendanceStatus attendanceStatus = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("All Statuses")) {
            attendanceStatus = AttendanceStatus.fromString(status.trim());
        }

        int pageSize = all ? 1000 : size;
        PagedResponse<AttendanceRecordDto> paged = attendanceService.getAttendanceRecords(
                search,
                employeeId,
                departmentId,
                date,
                startDate,
                endDate,
                attendanceStatus,
                page,
                pageSize,
                sortBy,
                sortDirection,
                authentication
        );

        if (all) {
            return ResponseEntity.ok(ApiResponse.ok("Attendance records retrieved successfully", paged.getContent()));
        }

        return ResponseEntity.ok(ApiResponse.ok("Attendance records retrieved successfully", paged));
    }

    /**
     * Retrieves an individual attendance record by UUID.
     * Enforces ownership validation for EMPLOYEE role.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AttendanceRecordDto>> getAttendanceById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        AttendanceRecordDto record = attendanceService.getAttendanceById(id, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Attendance record retrieved successfully", record));
    }

    /**
     * Records self-service employee check-in for today.
     * Derives identity securely from the authenticated principal.
     */
    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<AttendanceRecordDto>> checkIn(
            @RequestBody(required = false) AttendanceCheckInRequest request,
            Authentication authentication
    ) {
        AttendanceRecordDto record = attendanceService.checkIn(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Check-in recorded successfully", record));
    }

    /**
     * Records self-service employee check-out for today.
     * Derives identity securely from the authenticated principal.
     */
    @PostMapping("/check-out")
    public ResponseEntity<ApiResponse<AttendanceRecordDto>> checkOut(
            @RequestBody(required = false) AttendanceCheckOutRequest request,
            Authentication authentication
    ) {
        AttendanceRecordDto record = attendanceService.checkOut(request, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Check-out recorded successfully", record));
    }

    /**
     * Administrator manual attendance creation.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceRecordDto>> createManualAttendance(
            @Valid @RequestBody AttendanceCreateRequest request,
            Authentication authentication
    ) {
        AttendanceRecordDto record = attendanceService.createManualAttendance(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Attendance record created successfully", record));
    }

    /**
     * Administrator attendance update / status override.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceRecordDto>> updateAttendance(
            @PathVariable UUID id,
            @Valid @RequestBody AttendanceUpdateRequest request,
            Authentication authentication
    ) {
        AttendanceRecordDto record = attendanceService.updateAttendanceRecord(id, request, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Attendance record updated successfully", record));
    }

    /**
     * Retrieves aggregate summary metrics for daily attendance dashboard cards.
     */
    @GetMapping({"/summary", "/stats"})
    public ResponseEntity<ApiResponse<AttendanceSummaryDto>> getAttendanceSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication
    ) {
        AttendanceSummaryDto summary = attendanceService.getAttendanceSummary(date, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Attendance summary retrieved successfully", summary));
    }
}

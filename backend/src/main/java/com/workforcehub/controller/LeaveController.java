package com.workforcehub.controller;

import com.workforcehub.dto.*;
import com.workforcehub.entity.enums.LeaveStatus;
import com.workforcehub.entity.enums.LeaveType;
import com.workforcehub.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller managing leave request submissions, approvals, rejections,
 * policy allowances, and administrative queries.
 */
@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    @Autowired
    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /**
     * Lists leave requests with database-side filtering, sorting, and pagination.
     * Accessible by ADMIN (across all employees) and EMPLOYEE (own records only).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getLeaves(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String leaveType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submitted-desc") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "false") boolean all,
            Authentication authentication
    ) {
        LeaveStatus resolvedStatus = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("All Statuses") && !status.equalsIgnoreCase("ALL")) {
            try {
                resolvedStatus = LeaveStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Ignore unrecognized string or let filter return empty
            }
        }

        LeaveType resolvedType = null;
        if (leaveType != null && !leaveType.isBlank() && !leaveType.equalsIgnoreCase("All Types") && !leaveType.equalsIgnoreCase("ALL")) {
            try {
                resolvedType = LeaveType.valueOf(leaveType.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Ignore unrecognized string or let filter return empty
            }
        }

        int pageSize = all ? 1000 : size;
        PagedResponse<LeaveRequestDto> paged = leaveService.getLeaveRequests(
                search,
                employeeId,
                resolvedStatus,
                resolvedType,
                startDate,
                endDate,
                page,
                pageSize,
                sortBy,
                sortDirection,
                authentication
        );

        if (all) {
            return ResponseEntity.ok(ApiResponse.ok("Leave requests retrieved successfully", paged.getContent()));
        }

        return ResponseEntity.ok(ApiResponse.ok("Leave requests retrieved successfully", paged));
    }

    /**
     * Retrieves a single leave request by ID.
     * Enforces ownership: EMPLOYEE can only view their own; ADMIN can view company-wide.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> getLeaveById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        LeaveRequestDto dto = leaveService.getLeaveRequestById(id, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Leave request retrieved successfully", dto));
    }

    /**
     * Submits a leave request for the authenticated employee.
     * Client cannot spoof identity or approval status. Defaults to PENDING.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LeaveRequestDto>> applyLeave(
            @Valid @RequestBody LeaveApplyRequest request,
            Authentication authentication
    ) {
        LeaveRequestDto created = leaveService.applyLeave(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Leave request submitted successfully", created));
    }

    /**
     * Updates an existing pending leave request.
     * Employees can only update their own requests while still PENDING.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> updateLeave(
            @PathVariable UUID id,
            @Valid @RequestBody LeaveUpdateRequest request,
            Authentication authentication
    ) {
        LeaveRequestDto updated = leaveService.updateLeave(id, request, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Leave request updated successfully", updated));
    }

    /**
     * Approves a pending leave request.
     * ADMIN-only. Revalidates overlapping constraints and sets reviewer metadata.
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> approveLeave(
            @PathVariable UUID id,
            @RequestBody(required = false) LeaveReviewRequest request,
            Authentication authentication
    ) {
        LeaveRequestDto approved = leaveService.approveLeave(id, request, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Leave request approved successfully", approved));
    }

    /**
     * Rejects a pending leave request.
     * ADMIN-only. Rejection comment is validated and recorded.
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> rejectLeave(
            @PathVariable UUID id,
            @RequestBody(required = false) LeaveReviewRequest request,
            Authentication authentication
    ) {
        LeaveRequestDto rejected = leaveService.rejectLeave(id, request, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Leave request rejected successfully", rejected));
    }

    /**
     * Cancels an existing pending leave request.
     * Employees can cancel their own pending requests; ADMIN can cancel any pending request.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelLeave(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        leaveService.cancelLeave(id, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Leave request cancelled successfully", null));
    }

    /**
     * Retrieves leave balances for the authenticated employee.
     */
    @GetMapping("/balance/me")
    public ResponseEntity<ApiResponse<LeaveBalancesDto>> getMyBalances(
            @RequestParam(required = false) Integer year,
            Authentication authentication
    ) {
        LeaveBalancesDto balances = leaveService.getEmployeeBalances(null, year, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Leave balances retrieved successfully", balances));
    }

    /**
     * Retrieves leave balances for a specific employee.
     * Restricted to ADMIN role.
     */
    @GetMapping("/balance/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaveBalancesDto>> getEmployeeBalances(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) Integer year,
            Authentication authentication
    ) {
        LeaveBalancesDto balances = leaveService.getEmployeeBalances(employeeId, year, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Employee leave balances retrieved successfully", balances));
    }

    /**
     * Summary metrics for leave dashboard counters.
     * Accessible by authenticated users (ADMIN sees company metrics; EMPLOYEE sees personal summary).
     */
    @GetMapping({"/summary", "/stats"})
    public ResponseEntity<ApiResponse<LeaveSummaryDto>> getLeaveSummary(Authentication authentication) {
        LeaveSummaryDto summary = leaveService.getLeaveSummary(authentication);
        return ResponseEntity.ok(ApiResponse.ok("Leave summary retrieved successfully", summary));
    }
}

package com.workforcehub.service;

import com.workforcehub.dto.*;
import com.workforcehub.entity.enums.LeaveStatus;
import com.workforcehub.entity.enums.LeaveType;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service contract governing organizational leave applications, review workflows,
 * allowance balances, and attendance integration.
 */
public interface LeaveService {

    PagedResponse<LeaveRequestDto> getLeaveRequests(
            String search,
            UUID employeeId,
            LeaveStatus status,
            LeaveType leaveType,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy,
            String sortDirection,
            Authentication authentication
    );

    LeaveRequestDto getLeaveRequestById(UUID id, Authentication authentication);

    LeaveRequestDto applyLeave(LeaveApplyRequest request, Authentication authentication);

    LeaveRequestDto updateLeave(UUID id, LeaveUpdateRequest request, Authentication authentication);

    LeaveRequestDto approveLeave(UUID id, LeaveReviewRequest request, Authentication authentication);

    LeaveRequestDto rejectLeave(UUID id, LeaveReviewRequest request, Authentication authentication);

    void cancelLeave(UUID id, Authentication authentication);

    LeaveBalancesDto getEmployeeBalances(UUID employeeId, Integer year, Authentication authentication);

    LeaveSummaryDto getLeaveSummary(Authentication authentication);

    boolean isEmployeeOnApprovedLeave(UUID employeeId, LocalDate date);
}

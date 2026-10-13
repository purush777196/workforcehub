package com.workforcehub.service;

import com.workforcehub.dto.*;
import com.workforcehub.entity.enums.AttendanceStatus;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.UUID;

public interface AttendanceService {

    PagedResponse<AttendanceRecordDto> getAttendanceRecords(
            String search,
            UUID employeeId,
            UUID departmentId,
            LocalDate date,
            LocalDate startDate,
            LocalDate endDate,
            AttendanceStatus status,
            int page,
            int size,
            String sortBy,
            String sortDirection,
            Authentication authentication
    );

    AttendanceRecordDto getAttendanceById(UUID id, Authentication authentication);

    AttendanceRecordDto checkIn(AttendanceCheckInRequest request, Authentication authentication);

    AttendanceRecordDto checkOut(AttendanceCheckOutRequest request, Authentication authentication);

    AttendanceRecordDto createManualAttendance(AttendanceCreateRequest request, Authentication authentication);

    AttendanceRecordDto updateAttendanceRecord(UUID id, AttendanceUpdateRequest request, Authentication authentication);

    AttendanceSummaryDto getAttendanceSummary(LocalDate date, Authentication authentication);
}

package com.workforcehub.service.impl;

import com.workforcehub.dto.*;
import com.workforcehub.entity.AttendanceRecord;
import com.workforcehub.entity.Employee;
import com.workforcehub.entity.UserAccount;
import com.workforcehub.entity.enums.AttendanceStatus;
import com.workforcehub.entity.enums.EmploymentStatus;
import com.workforcehub.exception.BusinessRuleViolationException;
import com.workforcehub.exception.DuplicateResourceException;
import com.workforcehub.exception.ResourceNotFoundException;
import com.workforcehub.repository.AttendanceRecordRepository;
import com.workforcehub.repository.EmployeeRepository;
import com.workforcehub.repository.UserAccountRepository;
import com.workforcehub.service.AttendanceService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceServiceImpl.class);

    public static final LocalTime SHIFT_START_THRESHOLD = LocalTime.of(9, 15);

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final com.workforcehub.repository.LeaveRequestRepository leaveRequestRepository;

    @Autowired
    public AttendanceServiceImpl(
            AttendanceRecordRepository attendanceRecordRepository,
            EmployeeRepository employeeRepository,
            UserAccountRepository userAccountRepository,
            com.workforcehub.repository.LeaveRequestRepository leaveRequestRepository
    ) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    public AttendanceServiceImpl(
            AttendanceRecordRepository attendanceRecordRepository,
            EmployeeRepository employeeRepository,
            UserAccountRepository userAccountRepository
    ) {
        this(attendanceRecordRepository, employeeRepository, userAccountRepository, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AttendanceRecordDto> getAttendanceRecords(
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
    ) {
        int validPage = Math.max(page, 0);
        int validSize = Math.max(1, Math.min(size, 100));

        boolean isAdminUser = isAdmin(authentication);
        UUID targetEmployeeId = employeeId;

        // Non-admin employees are strictly restricted to their own attendance records
        if (!isAdminUser) {
            Employee currentEmp = resolveCurrentEmployee(authentication);
            if (employeeId != null && !employeeId.equals(currentEmp.getId())) {
                throw new AccessDeniedException("You do not have permission to view attendance records for other employees.");
            }
            targetEmployeeId = currentEmp.getId();
        }

        Sort sort = resolveSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(validPage, validSize, sort);

        UUID finalEmployeeId = targetEmployeeId;
        Specification<AttendanceRecord> spec = (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("employee", jakarta.persistence.criteria.JoinType.INNER)
                    .fetch("department", jakarta.persistence.criteria.JoinType.INNER);
            }

            List<Predicate> predicates = new ArrayList<>();

            // 1. Employee ID Filter
            if (finalEmployeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), finalEmployeeId));
            }

            // 2. Department ID Filter
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("employee").get("department").get("id"), departmentId));
            }

            // 3. Exact Date Filter
            if (date != null) {
                predicates.add(cb.equal(root.get("attendanceDate"), date));
            }

            // 4. Date Range Filter
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("attendanceDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("attendanceDate"), endDate));
            }

            // 5. Status Filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 6. Search Filter (first name, last name, code, email)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Join<AttendanceRecord, Employee> empJoin = root.join("employee");
                Predicate firstNameMatch = cb.like(cb.lower(empJoin.get("firstName")), searchPattern);
                Predicate lastNameMatch = cb.like(cb.lower(empJoin.get("lastName")), searchPattern);
                Predicate codeMatch = cb.like(cb.lower(empJoin.get("employeeCode")), searchPattern);
                Predicate emailMatch = cb.like(cb.lower(empJoin.get("email")), searchPattern);
                predicates.add(cb.or(firstNameMatch, lastNameMatch, codeMatch, emailMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AttendanceRecord> recordsPage = attendanceRecordRepository.findAll(spec, pageable);
        List<AttendanceRecordDto> content = recordsPage.getContent().stream()
                .map(AttendanceRecordDto::fromEntity)
                .toList();

        return new PagedResponse<>(
                content,
                recordsPage.getNumber(),
                recordsPage.getSize(),
                recordsPage.getTotalElements(),
                recordsPage.getTotalPages(),
                recordsPage.isFirst(),
                recordsPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceRecordDto getAttendanceById(UUID id, Authentication authentication) {
        AttendanceRecord record = attendanceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record with ID " + id + " not found"));

        if (!isAdmin(authentication)) {
            Employee currentEmp = resolveCurrentEmployee(authentication);
            if (!record.getEmployee().getId().equals(currentEmp.getId())) {
                throw new AccessDeniedException("You do not have permission to access another employee's attendance record.");
            }
        }

        return AttendanceRecordDto.fromEntity(record);
    }

    @Override
    public AttendanceRecordDto checkIn(AttendanceCheckInRequest request, Authentication authentication) {
        Employee employee = resolveCurrentEmployee(authentication);
        LocalDate today = LocalDate.now();

        if (employee.getEmploymentStatus() == EmploymentStatus.INACTIVE) {
            throw new BusinessRuleViolationException("Cannot check in: employee account is inactive.");
        }
        if (employee.getEmploymentStatus() == EmploymentStatus.ON_LEAVE || isEmployeeOnApprovedLeave(employee.getId(), today)) {
            throw new BusinessRuleViolationException("Cannot check in: employee is currently on approved leave.");
        }
        LocalTime checkInTime = (request != null && request.getCheckInTime() != null)
                ? request.getCheckInTime()
                : LocalTime.now().truncatedTo(ChronoUnit.SECONDS);

        Optional<AttendanceRecord> existingOpt = attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today);
        if (existingOpt.isPresent()) {
            AttendanceRecord existing = existingOpt.get();
            if (existing.getCheckInTime() != null) {
                throw new DuplicateResourceException(
                        "Employee " + employee.getFullName() + " has already checked in today at " + existing.getCheckInTime()
                );
            }
            // If an unfulfilled record existed (e.g. marked absent or pending), update it
            boolean isLate = checkInTime.isAfter(SHIFT_START_THRESHOLD);
            existing.setCheckInTime(checkInTime);
            existing.setStatus(isLate ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
            if (request != null && request.getNotes() != null && !request.getNotes().isBlank()) {
                existing.setNotes(request.getNotes().trim());
            }
            AttendanceRecord saved = attendanceRecordRepository.save(existing);
            log.info("Updated check-in for [{}] at [{}] status=[{}].", employee.getFullName(), checkInTime, saved.getStatus());
            return AttendanceRecordDto.fromEntity(saved);
        }

        boolean isLate = checkInTime.isAfter(SHIFT_START_THRESHOLD);
        AttendanceStatus status = isLate ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;

        AttendanceRecord newRecord = new AttendanceRecord(employee, today, checkInTime, status);
        if (request != null && request.getNotes() != null && !request.getNotes().isBlank()) {
            newRecord.setNotes(request.getNotes().trim());
        }

        try {
            AttendanceRecord saved = attendanceRecordRepository.save(newRecord);
            log.info("Recorded check-in for [{}] at [{}] status=[{}].", employee.getFullName(), checkInTime, status);
            return AttendanceRecordDto.fromEntity(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent duplicate check-in detected for employee [{}] on date [{}].", employee.getId(), today);
            throw new DuplicateResourceException("Employee has already checked in for today.");
        }
    }

    @Override
    public AttendanceRecordDto checkOut(AttendanceCheckOutRequest request, Authentication authentication) {
        Employee employee = resolveCurrentEmployee(authentication);
        LocalDate today = LocalDate.now();
        LocalTime checkOutTime = (request != null && request.getCheckOutTime() != null)
                ? request.getCheckOutTime()
                : LocalTime.now().truncatedTo(ChronoUnit.SECONDS);

        AttendanceRecord record = attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Cannot check out: no attendance record found for " + employee.getFullName() + " today. You must check in first."
                ));

        if (record.getCheckInTime() == null) {
            throw new BusinessRuleViolationException("Cannot check out: no check-in timestamp found for today.");
        }

        if (record.getCheckOutTime() != null) {
            throw new BusinessRuleViolationException(
                    "Employee " + employee.getFullName() + " has already checked out today at " + record.getCheckOutTime()
            );
        }

        if (checkOutTime.isBefore(record.getCheckInTime())) {
            throw new BusinessRuleViolationException("Check-out time cannot be earlier than check-in time.");
        }

        record.setCheckOutTime(checkOutTime);
        if (request != null && request.getNotes() != null && !request.getNotes().isBlank()) {
            String existingNotes = record.getNotes() != null ? record.getNotes() + " | " : "";
            record.setNotes(existingNotes + request.getNotes().trim());
        }

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        log.info("Recorded check-out for [{}] at [{}].", employee.getFullName(), checkOutTime);
        return AttendanceRecordDto.fromEntity(saved);
    }

    @Override
    public AttendanceRecordDto createManualAttendance(AttendanceCreateRequest request, Authentication authentication) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID " + request.getEmployeeId() + " not found"));

        if (attendanceRecordRepository.existsByEmployeeIdAndAttendanceDate(employee.getId(), request.getAttendanceDate())) {
            throw new DuplicateResourceException(
                    "An attendance record already exists for " + employee.getFullName() + " on " + request.getAttendanceDate() + ". Please update the existing record instead."
            );
        }

        LocalTime inTime = request.getCheckInTime();
        LocalTime outTime = request.getCheckOutTime();

        if (request.getStatus() == AttendanceStatus.ABSENT) {
            inTime = null;
            outTime = null;
        } else if (inTime == null) {
            inTime = LocalTime.of(9, 0);
        }

        if (inTime != null && outTime != null && outTime.isBefore(inTime)) {
            throw new BusinessRuleViolationException("Check-out time cannot precede check-in time.");
        }

        AttendanceRecord record = new AttendanceRecord(employee, request.getAttendanceDate(), inTime, request.getStatus());
        record.setCheckOutTime(outTime);
        if (request.getNotes() != null) {
            record.setNotes(request.getNotes().trim());
        }

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        log.info("Administrator created manual attendance for [{}] on [{}] status=[{}].", employee.getFullName(), request.getAttendanceDate(), request.getStatus());
        return AttendanceRecordDto.fromEntity(saved);
    }

    @Override
    public AttendanceRecordDto updateAttendanceRecord(UUID id, AttendanceUpdateRequest request, Authentication authentication) {
        AttendanceRecord record = attendanceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record with ID " + id + " not found"));

        LocalTime inTime = request.getCheckInTime() != null ? request.getCheckInTime() : record.getCheckInTime();
        LocalTime outTime = request.getCheckOutTime() != null ? request.getCheckOutTime() : record.getCheckOutTime();

        if (request.getStatus() == AttendanceStatus.ABSENT) {
            inTime = null;
            outTime = null;
        } else if (inTime == null) {
            inTime = LocalTime.of(9, 0);
        }

        if (inTime != null && outTime != null && outTime.isBefore(inTime)) {
            throw new BusinessRuleViolationException("Check-out time cannot precede check-in time.");
        }

        record.setStatus(request.getStatus());
        record.setCheckInTime(inTime);
        record.setCheckOutTime(outTime);
        if (request.getNotes() != null) {
            record.setNotes(request.getNotes().trim());
        }

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        log.info("Administrator updated attendance record [{}] status=[{}].", id, request.getStatus());
        return AttendanceRecordDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryDto getAttendanceSummary(LocalDate date, Authentication authentication) {
        LocalDate queryDate = date != null ? date : LocalDate.now();

        long totalEmployees = employeeRepository.count();
        long inactiveCount = employeeRepository.countByEmploymentStatus(EmploymentStatus.INACTIVE);
        long statusOnLeaveCount = employeeRepository.countByEmploymentStatus(EmploymentStatus.ON_LEAVE);
        long approvedLeavesOnDate = leaveRequestRepository != null ? leaveRequestRepository.countApprovedEmployeesOnDate(queryDate) : 0L;
        long onLeaveCount = Math.max(statusOnLeaveCount, approvedLeavesOnDate);
        long totalEligible = Math.max(0, totalEmployees - inactiveCount);

        long presentCount = attendanceRecordRepository.countByAttendanceDateAndStatus(queryDate, AttendanceStatus.PRESENT);
        long lateCount = attendanceRecordRepository.countByAttendanceDateAndStatus(queryDate, AttendanceStatus.LATE);
        long halfDayCount = attendanceRecordRepository.countByAttendanceDateAndStatus(queryDate, AttendanceStatus.HALF_DAY);
        long explicitlyAbsent = attendanceRecordRepository.countByAttendanceDateAndStatus(queryDate, AttendanceStatus.ABSENT);

        long totalAttending = presentCount + lateCount + halfDayCount;
        long activeUnmarkedAbsent = Math.max(0, totalEligible - onLeaveCount - (totalAttending + explicitlyAbsent));
        long totalAbsent = explicitlyAbsent + activeUnmarkedAbsent;

        long denominator = Math.max(0, totalEligible - onLeaveCount);
        int attendanceRate = denominator > 0 ? (int) Math.round(((double) totalAttending / denominator) * 100) : 0;

        return new AttendanceSummaryDto(
                queryDate,
                totalEligible,
                totalEmployees,
                totalAttending,
                presentCount,
                lateCount,
                halfDayCount,
                totalAbsent,
                onLeaveCount,
                attendanceRate
        );
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Employee resolveCurrentEmployee(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication credentials required.");
        }

        String email = authentication.getName().trim().toLowerCase();

        // 1. Direct email lookup on employees
        Optional<Employee> empOpt = employeeRepository.findByEmailIgnoreCase(email);
        if (empOpt.isPresent()) {
            return empOpt.get();
        }

        // 2. Link via user account
        Optional<UserAccount> accountOpt = userAccountRepository.findByEmailIgnoreCase(email);
        if (accountOpt.isPresent()) {
            Optional<Employee> linkedEmp = employeeRepository.findByUserAccountId(accountOpt.get().getId());
            if (linkedEmp.isPresent()) {
                return linkedEmp.get();
            }
        }

        throw new ResourceNotFoundException("No employee profile found associated with the authenticated account '" + email + "'");
    }

    private Sort resolveSort(String sortBy, String sortDirection) {
        Sort.Direction direction = Sort.Direction.ASC;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
            direction = Sort.Direction.DESC;
        }

        if (sortBy == null || sortBy.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "attendanceDate").and(Sort.by(Sort.Direction.ASC, "checkInTime"));
        }

        return switch (sortBy.toLowerCase()) {
            case "name-desc" -> Sort.by(Sort.Direction.DESC, "employee.firstName", "employee.lastName");
            case "name-asc", "name" -> Sort.by(Sort.Direction.ASC, "employee.firstName", "employee.lastName");
            case "checkin-desc", "time-desc" -> Sort.by(Sort.Direction.DESC, "checkInTime");
            case "checkin-asc", "time-asc", "checkintime" -> Sort.by(Sort.Direction.ASC, "checkInTime");
            case "date-desc" -> Sort.by(Sort.Direction.DESC, "attendanceDate");
            case "date-asc", "date", "attendancedate" -> Sort.by(Sort.Direction.ASC, "attendanceDate");
            case "status" -> Sort.by(direction, "status");
            default -> Sort.by(Sort.Direction.DESC, "attendanceDate").and(Sort.by(direction, "checkInTime"));
        };
    }

    private boolean isEmployeeOnApprovedLeave(UUID employeeId, LocalDate date) {
        if (leaveRequestRepository == null || employeeId == null || date == null) {
            return false;
        }
        List<com.workforcehub.entity.LeaveRequest> approvedLeaves = leaveRequestRepository.findApprovedLeavesOnDate(employeeId, date);
        return approvedLeaves != null && !approvedLeaves.isEmpty();
    }
}

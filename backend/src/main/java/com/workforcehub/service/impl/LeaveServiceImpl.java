package com.workforcehub.service.impl;

import com.workforcehub.dto.*;
import com.workforcehub.entity.Employee;
import com.workforcehub.entity.LeaveRequest;
import com.workforcehub.entity.UserAccount;
import com.workforcehub.entity.enums.EmploymentStatus;
import com.workforcehub.entity.enums.LeaveStatus;
import com.workforcehub.entity.enums.LeaveType;
import com.workforcehub.exception.BusinessRuleViolationException;
import com.workforcehub.exception.DuplicateResourceException;
import com.workforcehub.exception.ResourceNotFoundException;
import com.workforcehub.repository.EmployeeRepository;
import com.workforcehub.repository.LeaveRequestRepository;
import com.workforcehub.repository.UserAccountRepository;
import com.workforcehub.service.LeaveService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class LeaveServiceImpl implements LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveServiceImpl.class);

    // Standard Leave Policies (calendar days per year)
    public static final int ANNUAL_LEAVE_ALLOWANCE = 20;
    public static final int SICK_LEAVE_ALLOWANCE = 12;
    public static final int PERSONAL_LEAVE_ALLOWANCE = 5;
    public static final int UNPAID_LEAVE_ALLOWANCE = 30;

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;

    @Autowired
    public LeaveServiceImpl(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public static int getPolicyAllowance(LeaveType type) {
        return switch (type) {
            case ANNUAL -> ANNUAL_LEAVE_ALLOWANCE;
            case SICK -> SICK_LEAVE_ALLOWANCE;
            case PERSONAL -> PERSONAL_LEAVE_ALLOWANCE;
            case UNPAID -> UNPAID_LEAVE_ALLOWANCE;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<LeaveRequestDto> getLeaveRequests(
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
    ) {
        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, 1000));

        UUID targetEmployeeId = employeeId;
        boolean isAdminUser = isAdmin(authentication);

        if (!isAdminUser) {
            Employee currentEmp = resolveCurrentEmployee(authentication);
            if (employeeId != null && !employeeId.equals(currentEmp.getId())) {
                throw new AccessDeniedException("You do not have permission to view leave requests for other employees.");
            }
            targetEmployeeId = currentEmp.getId();
        }

        Sort sort = resolveSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(validPage, validSize, sort);

        UUID finalEmployeeId = targetEmployeeId;
        Specification<LeaveRequest> spec = (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("employee", JoinType.INNER)
                    .fetch("department", JoinType.LEFT);
                root.fetch("reviewedBy", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (finalEmployeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), finalEmployeeId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (leaveType != null) {
                predicates.add(cb.equal(root.get("leaveType"), leaveType));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), endDate));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchEmpFirst = cb.like(cb.lower(root.get("employee").get("firstName")), pattern);
                Predicate searchEmpLast = cb.like(cb.lower(root.get("employee").get("lastName")), pattern);
                Predicate searchEmpCode = cb.like(cb.lower(root.get("employee").get("employeeCode")), pattern);
                Predicate searchReason = cb.like(cb.lower(root.get("reason")), pattern);
                predicates.add(cb.or(searchEmpFirst, searchEmpLast, searchEmpCode, searchReason));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<LeaveRequest> pageResult = leaveRequestRepository.findAll(spec, pageable);
        List<LeaveRequestDto> dtoList = pageResult.getContent().stream()
                .map(LeaveRequestDto::fromEntity)
                .toList();

        return new PagedResponse<>(
                dtoList,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isFirst(),
                pageResult.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveRequestDto getLeaveRequestById(UUID id, Authentication authentication) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request with ID " + id + " not found"));

        if (!isAdmin(authentication)) {
            Employee currentEmp = resolveCurrentEmployee(authentication);
            if (!request.getEmployee().getId().equals(currentEmp.getId())) {
                throw new AccessDeniedException("You do not have permission to view this leave request.");
            }
        }

        return LeaveRequestDto.fromEntity(request);
    }

    @Override
    public LeaveRequestDto applyLeave(LeaveApplyRequest request, Authentication authentication) {
        Employee employee;
        if (isAdmin(authentication) && request.getEmployeeId() != null) {
            employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee with ID " + request.getEmployeeId() + " not found"));
        } else {
            employee = resolveCurrentEmployee(authentication);
        }

        if (employee.getEmploymentStatus() == EmploymentStatus.INACTIVE) {
            throw new BusinessRuleViolationException("Cannot apply for leave: employee account is inactive.");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessRuleViolationException("End date cannot precede start date.");
        }

        int daysCount = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (daysCount <= 0) {
            throw new BusinessRuleViolationException("Invalid date range specified.");
        }

        // Check overlapping Pending or Approved requests
        List<LeaveRequest> conflicts = leaveRequestRepository.findConflictingRequests(
                employee.getId(),
                request.getStartDate(),
                request.getEndDate(),
                null
        );
        if (!conflicts.isEmpty()) {
            LeaveRequest conflict = conflicts.get(0);
            String statusLabel = conflict.getStatus() == LeaveStatus.APPROVED ? "an approved" : "a pending";
            throw new DuplicateResourceException(
                    "Conflict: " + employee.getFullName() + " already has " + statusLabel + " request from " +
                    conflict.getStartDate() + " to " + conflict.getEndDate() + "."
            );
        }

        // Check balance if Annual leave
        if (request.getLeaveType() == LeaveType.ANNUAL) {
            int year = request.getStartDate().getYear();
            int remaining = calculateRemainingDays(employee.getId(), LeaveType.ANNUAL, year);
            if (daysCount > remaining) {
                throw new BusinessRuleViolationException(
                        "Insufficient Annual Leave: requested " + daysCount + " calendar days exceeds remaining allowance (" +
                        remaining + " days available)."
                );
            }
        }

        LeaveRequest newRequest = new LeaveRequest(
                employee,
                request.getLeaveType(),
                request.getStartDate(),
                request.getEndDate(),
                daysCount,
                request.getReason().trim()
        );
        newRequest.setStatus(LeaveStatus.PENDING);

        LeaveRequest saved = leaveRequestRepository.save(newRequest);
        log.info("Leave request created [{}] for [{}] ({} to {}, {} days, status=PENDING).",
                saved.getId(), employee.getFullName(), saved.getStartDate(), saved.getEndDate(), daysCount);

        return LeaveRequestDto.fromEntity(saved);
    }

    @Override
    public LeaveRequestDto updateLeave(UUID id, LeaveUpdateRequest request, Authentication authentication) {
        LeaveRequest existing = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request with ID " + id + " not found"));

        if (!isAdmin(authentication)) {
            Employee currentEmp = resolveCurrentEmployee(authentication);
            if (!existing.getEmployee().getId().equals(currentEmp.getId())) {
                throw new AccessDeniedException("You do not have permission to modify this leave request.");
            }
        }

        if (existing.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessRuleViolationException("Only Pending leave requests can be edited. Current status: " + existing.getStatus());
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessRuleViolationException("End date cannot precede start date.");
        }

        int daysCount = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (daysCount <= 0) {
            throw new BusinessRuleViolationException("Invalid date range specified.");
        }

        List<LeaveRequest> conflicts = leaveRequestRepository.findConflictingRequests(
                existing.getEmployee().getId(),
                request.getStartDate(),
                request.getEndDate(),
                existing.getId()
        );
        if (!conflicts.isEmpty()) {
            LeaveRequest conflict = conflicts.get(0);
            String statusLabel = conflict.getStatus() == LeaveStatus.APPROVED ? "an approved" : "a pending";
            throw new DuplicateResourceException(
                    "Conflict: Overlaps with " + statusLabel + " request (" + conflict.getStartDate() + " to " + conflict.getEndDate() + ")."
            );
        }

        if (request.getLeaveType() == LeaveType.ANNUAL) {
            int year = request.getStartDate().getYear();
            int remaining = calculateRemainingDays(existing.getEmployee().getId(), LeaveType.ANNUAL, year);
            if (daysCount > remaining) {
                throw new BusinessRuleViolationException(
                        "Request exceeds remaining Annual Leave allowance (" + remaining + " days available)."
                );
            }
        }

        existing.setLeaveType(request.getLeaveType());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        existing.setDaysCount(daysCount);
        existing.setReason(request.getReason().trim());

        LeaveRequest saved = leaveRequestRepository.save(existing);
        log.info("Leave request updated [{}] for [{}] ({} to {}, {} days).",
                saved.getId(), saved.getEmployee().getFullName(), saved.getStartDate(), saved.getEndDate(), daysCount);

        return LeaveRequestDto.fromEntity(saved);
    }

    @Override
    public LeaveRequestDto approveLeave(UUID id, LeaveReviewRequest request, Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw new AccessDeniedException("Only administrators may approve leave requests.");
        }

        LeaveRequest existing = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request with ID " + id + " not found"));

        if (existing.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessRuleViolationException("Only Pending leave requests can be approved. Current status: " + existing.getStatus());
        }

        // Recheck overlaps against approved requests
        List<LeaveRequest> approvedConflicts = leaveRequestRepository.findOverlappingRequests(
                existing.getEmployee().getId(),
                LeaveStatus.APPROVED,
                existing.getStartDate(),
                existing.getEndDate()
        );
        if (!approvedConflicts.isEmpty()) {
            LeaveRequest c = approvedConflicts.get(0);
            throw new BusinessRuleViolationException(
                    "Cannot approve: Employee already has an approved request (" + c.getStartDate() + " to " + c.getEndDate() + ")."
            );
        }

        // Recheck annual leave balance
        if (existing.getLeaveType() == LeaveType.ANNUAL) {
            int year = existing.getStartDate().getYear();
            int remaining = calculateRemainingDays(existing.getEmployee().getId(), LeaveType.ANNUAL, year);
            if (existing.getDaysCount() > remaining) {
                throw new BusinessRuleViolationException(
                        "Cannot approve: Employee only has " + remaining + " Annual Leave days remaining (" + existing.getDaysCount() + " requested)."
                );
            }
        }

        Employee reviewer = resolveCurrentEmployee(authentication);
        String comment = (request != null && request.getReviewComment() != null && !request.getReviewComment().isBlank())
                ? request.getReviewComment().trim()
                : "Approved by HR Administrator";

        existing.setStatus(LeaveStatus.APPROVED);
        existing.setReviewedBy(reviewer);
        existing.setReviewedAt(Instant.now());
        existing.setReviewComment(comment);

        // Update employee status if leave covers today
        LocalDate today = LocalDate.now();
        if (!today.isBefore(existing.getStartDate()) && !today.isAfter(existing.getEndDate())) {
            existing.getEmployee().setEmploymentStatus(EmploymentStatus.ON_LEAVE);
            employeeRepository.save(existing.getEmployee());
            log.info("Updated employee [{}] employment status to ON_LEAVE.", existing.getEmployee().getFullName());
        }

        LeaveRequest saved = leaveRequestRepository.save(existing);
        log.info("Administrator [{}] approved leave [{}] for [{}] ({} to {}).",
                reviewer.getFullName(), saved.getId(), saved.getEmployee().getFullName(), saved.getStartDate(), saved.getEndDate());

        return LeaveRequestDto.fromEntity(saved);
    }

    @Override
    public LeaveRequestDto rejectLeave(UUID id, LeaveReviewRequest request, Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw new AccessDeniedException("Only administrators may reject leave requests.");
        }

        LeaveRequest existing = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request with ID " + id + " not found"));

        if (existing.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessRuleViolationException("Only Pending leave requests can be rejected. Current status: " + existing.getStatus());
        }

        if (request == null || request.getReviewComment() == null || request.getReviewComment().isBlank()) {
            throw new BusinessRuleViolationException("A rejection reason / comment is required.");
        }

        Employee reviewer = resolveCurrentEmployee(authentication);

        existing.setStatus(LeaveStatus.REJECTED);
        existing.setReviewedBy(reviewer);
        existing.setReviewedAt(Instant.now());
        existing.setReviewComment(request.getReviewComment().trim());

        LeaveRequest saved = leaveRequestRepository.save(existing);
        log.info("Administrator [{}] rejected leave [{}] for [{}]. Reason: {}",
                reviewer.getFullName(), saved.getId(), saved.getEmployee().getFullName(), saved.getReviewComment());

        return LeaveRequestDto.fromEntity(saved);
    }

    @Override
    public void cancelLeave(UUID id, Authentication authentication) {
        LeaveRequest existing = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request with ID " + id + " not found"));

        if (!isAdmin(authentication)) {
            Employee currentEmp = resolveCurrentEmployee(authentication);
            if (!existing.getEmployee().getId().equals(currentEmp.getId())) {
                throw new AccessDeniedException("You do not have permission to cancel this leave request.");
            }
        }

        if (existing.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessRuleViolationException("Only Pending leave requests can be cancelled. Current status: " + existing.getStatus());
        }

        leaveRequestRepository.delete(existing);
        log.info("Cancelled pending leave request [{}] for [{}].", id, existing.getEmployee().getFullName());
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveBalancesDto getEmployeeBalances(UUID employeeId, Integer year, Authentication authentication) {
        Employee employee;
        if (employeeId != null) {
            if (!isAdmin(authentication)) {
                Employee currentEmp = resolveCurrentEmployee(authentication);
                if (!employeeId.equals(currentEmp.getId())) {
                    throw new AccessDeniedException("You do not have permission to view leave balances for other employees.");
                }
            }
            employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee with ID " + employeeId + " not found"));
        } else {
            employee = resolveCurrentEmployee(authentication);
        }

        int targetYear = year != null ? year : LocalDate.now().getYear();
        LocalDate startOfYear = LocalDate.of(targetYear, 1, 1);
        LocalDate endOfYear = LocalDate.of(targetYear, 12, 31);

        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findApprovedLeavesInDateRange(
                employee.getId(),
                startOfYear,
                endOfYear
        );

        Map<String, LeaveBalanceItemDto> balancesMap = new LinkedHashMap<>();
        for (LeaveType type : LeaveType.values()) {
            int usedDays = approvedLeaves.stream()
                    .filter(r -> r.getLeaveType() == type)
                    .mapToInt(LeaveRequest::getDaysCount)
                    .sum();

            int allowance = getPolicyAllowance(type);
            int remainingDays = Math.max(0, allowance - usedDays);
            boolean isExceeded = usedDays > allowance;

            String label = type.getDisplayValue(); // "Annual", "Sick", "Personal", "Unpaid"
            balancesMap.put(label, new LeaveBalanceItemDto(label, allowance, usedDays, remainingDays, isExceeded));
        }

        return new LeaveBalancesDto(
                employee.getId(),
                employee.getFullName(),
                employee.getEmployeeCode(),
                targetYear,
                balancesMap
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveSummaryDto getLeaveSummary(Authentication authentication) {
        long totalRequests = leaveRequestRepository.count();
        long pendingCount = leaveRequestRepository.countByStatus(LeaveStatus.PENDING);
        long approvedCount = leaveRequestRepository.countByStatus(LeaveStatus.APPROVED);
        long rejectedCount = leaveRequestRepository.countByStatus(LeaveStatus.REJECTED);
        long todayOnLeaveCount = leaveRequestRepository.countApprovedEmployeesOnDate(LocalDate.now());

        return new LeaveSummaryDto(
                totalRequests,
                pendingCount,
                approvedCount,
                rejectedCount,
                todayOnLeaveCount
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmployeeOnApprovedLeave(UUID employeeId, LocalDate date) {
        if (employeeId == null || date == null) return false;
        return !leaveRequestRepository.findApprovedLeavesOnDate(employeeId, date).isEmpty();
    }

    private int calculateRemainingDays(UUID employeeId, LeaveType type, int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        List<LeaveRequest> approved = leaveRequestRepository.findApprovedLeavesInDateRange(employeeId, startOfYear, endOfYear);

        int used = approved.stream()
                .filter(r -> r.getLeaveType() == type)
                .mapToInt(LeaveRequest::getDaysCount)
                .sum();

        int allowance = getPolicyAllowance(type);
        return Math.max(0, allowance - used);
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

        Optional<Employee> empOpt = employeeRepository.findByEmailIgnoreCase(email);
        if (empOpt.isPresent()) {
            return empOpt.get();
        }

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
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return switch (sortBy.toLowerCase()) {
            case "name-desc" -> Sort.by(Sort.Direction.DESC, "employee.firstName", "employee.lastName");
            case "name-asc", "name" -> Sort.by(Sort.Direction.ASC, "employee.firstName", "employee.lastName");
            case "start-desc", "startdate-desc" -> Sort.by(Sort.Direction.DESC, "startDate");
            case "start-asc", "startdate-asc", "startdate" -> Sort.by(Sort.Direction.ASC, "startDate");
            case "submitted-desc", "createdat-desc" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "submitted-asc", "createdat-asc" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "status" -> Sort.by(direction, "status");
            case "leavetype", "type" -> Sort.by(direction, "leaveType");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}

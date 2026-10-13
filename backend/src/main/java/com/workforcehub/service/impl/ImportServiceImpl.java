package com.workforcehub.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workforcehub.dto.importing.*;
import com.workforcehub.entity.*;
import com.workforcehub.entity.enums.*;
import com.workforcehub.exception.BusinessRuleViolationException;
import com.workforcehub.exception.ResourceNotFoundException;
import com.workforcehub.repository.*;
import com.workforcehub.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ImportServiceImpl implements ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportServiceImpl.class);

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ImportJobRepository importJobRepository;
    private final ImportManifestRepository importManifestRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    public ImportServiceImpl(
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            LeaveRequestRepository leaveRequestRepository,
            ImportJobRepository importJobRepository,
            ImportManifestRepository importManifestRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.importJobRepository = importJobRepository;
        this.importManifestRepository = importManifestRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ImportPreviewResponseDto previewImport(ImportPayloadDto payload, Authentication auth) {
        validateAdminAuth(auth);
        String adminEmail = auth.getName();
        log.info("Generating safe import preview requested by administrator [{}]", adminEmail);

        if (payload == null) {
            payload = new ImportPayloadDto();
        }

        UUID previewId = UUID.randomUUID();
        ImportPreviewResponseDto response = new ImportPreviewResponseDto();
        response.setPreviewId(previewId);
        response.setGeneratedAt(Instant.now().toString());

        // 1. Departments Preview
        ModulePreviewSummaryDto deptSummary = previewDepartments(payload.getDepartments());
        response.setDepartments(deptSummary);

        // Build tentative set of available department codes & names (DB + payload eligible)
        Set<String> availableDeptCodes = new HashSet<>();
        Set<String> availableDeptNames = new HashSet<>();
        departmentRepository.findAll().forEach(d -> {
            availableDeptCodes.add(d.getDepartmentCode().trim().toUpperCase());
            availableDeptNames.add(d.getName().trim().toLowerCase());
        });
        for (RawDepartmentDto d : payload.getDepartments()) {
            if (d.getDepartmentId() != null) availableDeptCodes.add(d.getDepartmentId().trim().toUpperCase());
            if (d.getName() != null) availableDeptNames.add(d.getName().trim().toLowerCase());
        }

        // 2. Employees Preview
        ModulePreviewSummaryDto empSummary = previewEmployees(payload.getEmployees(), availableDeptCodes, availableDeptNames);
        response.setEmployees(empSummary);

        // Build tentative set of available employees (DB + payload)
        Map<String, String> sourceEmpIdToCode = new HashMap<>();
        Set<String> availableEmpCodes = new HashSet<>();
        employeeRepository.findAll().forEach(e -> {
            availableEmpCodes.add(e.getEmployeeCode().trim().toUpperCase());
        });
        for (RawEmployeeDto e : payload.getEmployees()) {
            if (e.getEmployeeId() != null) {
                String code = e.getEmployeeId().trim().toUpperCase();
                availableEmpCodes.add(code);
                if (e.getId() != null) {
                    sourceEmpIdToCode.put(e.getId().trim(), code);
                }
            }
        }

        // 3. Attendance Preview
        ModulePreviewSummaryDto attSummary = previewAttendance(payload.getAttendance(), availableEmpCodes, sourceEmpIdToCode);
        response.setAttendance(attSummary);

        // 4. Leaves Preview
        ModulePreviewSummaryDto leaveSummary = previewLeaves(payload.getLeaves(), availableEmpCodes, sourceEmpIdToCode);
        response.setLeaves(leaveSummary);

        // Aggregate counts
        int totalSource = deptSummary.getTotalSourceRecords() + empSummary.getTotalSourceRecords() +
                attSummary.getTotalSourceRecords() + leaveSummary.getTotalSourceRecords();
        int totalEligible = deptSummary.getEligibleForImport() + empSummary.getEligibleForImport() +
                attSummary.getEligibleForImport() + leaveSummary.getEligibleForImport();
        int totalExisting = deptSummary.getExistingInDatabase() + empSummary.getExistingInDatabase() +
                attSummary.getExistingInDatabase() + leaveSummary.getExistingInDatabase();
        int totalConflicts = deptSummary.getConflicts() + empSummary.getConflicts() +
                attSummary.getConflicts() + leaveSummary.getConflicts();
        int totalInvalid = deptSummary.getInvalidRecords() + empSummary.getInvalidRecords() +
                attSummary.getInvalidRecords() + leaveSummary.getInvalidRecords();
        int totalUnresolved = deptSummary.getUnresolvedReferences() + empSummary.getUnresolvedReferences() +
                attSummary.getUnresolvedReferences() + leaveSummary.getUnresolvedReferences();

        response.setTotalSourceAll(totalSource);
        response.setTotalEligibleAll(totalEligible);
        response.setTotalExistingAll(totalExisting);
        response.setTotalConflictsAll(totalConflicts);
        response.setTotalInvalidAll(totalInvalid);
        response.setTotalUnresolvedAll(totalUnresolved);
        response.setReadyForExecution(totalEligible > 0 || totalExisting > 0);

        return response;
    }

    private ModulePreviewSummaryDto previewDepartments(List<RawDepartmentDto> departments) {
        ModulePreviewSummaryDto summary = new ModulePreviewSummaryDto("departments");
        summary.setTotalSourceRecords(departments.size());

        for (RawDepartmentDto dept : departments) {
            String code = dept.getDepartmentId() != null ? dept.getDepartmentId().trim() : null;
            String name = dept.getName() != null ? dept.getName().trim() : null;

            if (code == null || code.isEmpty() || name == null || name.isEmpty()) {
                summary.setInvalidRecords(summary.getInvalidRecords() + 1);
                summary.addIssue(new ImportRecordIssueDto(dept.getId(), code != null ? code : "UNKNOWN", "INVALID", "Department code and name are mandatory"));
                continue;
            }

            Optional<Department> byCode = departmentRepository.findByDepartmentCode(code);
            Optional<Department> byName = departmentRepository.findByNameIgnoreCase(name);

            if (byCode.isPresent() && byName.isPresent()) {
                summary.setExistingInDatabase(summary.getExistingInDatabase() + 1);
                summary.addIssue(new ImportRecordIssueDto(dept.getId(), code, "EXISTING", "Department '" + name + "' (" + code + ") already exists in database (exact match). Will be mapped to existing database record."));
            } else if (byCode.isPresent() || byName.isPresent()) {
                summary.setConflicts(summary.getConflicts() + 1);
                String msg = byCode.isPresent()
                        ? "Department code '" + code + "' already exists with different name '" + byCode.get().getName() + "'"
                        : "Department name '" + name + "' already exists with different code '" + byName.get().getDepartmentCode() + "'";
                summary.addIssue(new ImportRecordIssueDto(dept.getId(), code, "CONFLICT", msg));
            } else {
                summary.setValidRecords(summary.getValidRecords() + 1);
                summary.setEligibleForImport(summary.getEligibleForImport() + 1);
            }
        }
        return summary;
    }

    private ModulePreviewSummaryDto previewEmployees(List<RawEmployeeDto> employees, Set<String> availableDeptCodes, Set<String> availableDeptNames) {
        ModulePreviewSummaryDto summary = new ModulePreviewSummaryDto("employees");
        summary.setTotalSourceRecords(employees.size());

        for (RawEmployeeDto emp : employees) {
            String code = emp.getEmployeeId() != null ? emp.getEmployeeId().trim() : null;
            String email = emp.getEmail() != null ? emp.getEmail().trim().toLowerCase() : null;
            String first = emp.getFirstName() != null ? emp.getFirstName().trim() : null;
            String last = emp.getLastName() != null ? emp.getLastName().trim() : null;
            String deptRef = emp.getDepartment() != null ? emp.getDepartment().trim() : null;

            if (code == null || code.isEmpty() || email == null || email.isEmpty() || first == null || first.isEmpty() || last == null || last.isEmpty()) {
                summary.setInvalidRecords(summary.getInvalidRecords() + 1);
                summary.addIssue(new ImportRecordIssueDto(emp.getId(), code != null ? code : "UNKNOWN", "INVALID", "Employee code, email, first name, and last name are mandatory"));
                continue;
            }

            // Department resolution check
            boolean deptFound = deptRef != null && (availableDeptCodes.contains(deptRef.toUpperCase()) || availableDeptNames.contains(deptRef.toLowerCase()));
            if (!deptFound) {
                summary.setUnresolvedReferences(summary.getUnresolvedReferences() + 1);
                summary.addIssue(new ImportRecordIssueDto(emp.getId(), code, "UNRESOLVED_REF", "Referenced department '" + deptRef + "' does not exist in database or import payload"));
                continue;
            }

            Optional<Employee> byCode = employeeRepository.findByEmployeeCode(code);
            Optional<Employee> byEmail = employeeRepository.findByEmailIgnoreCase(email);

            if (byCode.isPresent() && byEmail.isPresent()) {
                summary.setExistingInDatabase(summary.getExistingInDatabase() + 1);
                summary.addIssue(new ImportRecordIssueDto(emp.getId(), code, "EXISTING", "Employee [" + first + " " + last + "] (" + code + ") already exists in database (exact match). Will be linked to existing profile."));
            } else if (byCode.isPresent() || byEmail.isPresent()) {
                summary.setConflicts(summary.getConflicts() + 1);
                String msg = byCode.isPresent()
                        ? "Employee code '" + code + "' already assigned to '" + byCode.get().getFirstName() + " " + byCode.get().getLastName() + "'"
                        : "Email address '" + email + "' already in use by employee '" + byEmail.get().getEmployeeCode() + "'";
                summary.addIssue(new ImportRecordIssueDto(emp.getId(), code, "CONFLICT", msg));
            } else {
                summary.setValidRecords(summary.getValidRecords() + 1);
                summary.setEligibleForImport(summary.getEligibleForImport() + 1);
            }
        }
        return summary;
    }

    private ModulePreviewSummaryDto previewAttendance(List<RawAttendanceDto> records, Set<String> availableEmpCodes, Map<String, String> sourceEmpIdToCode) {
        ModulePreviewSummaryDto summary = new ModulePreviewSummaryDto("attendance");
        summary.setTotalSourceRecords(records.size());

        for (RawAttendanceDto att : records) {
            String empRef = att.getEmployeeId() != null ? att.getEmployeeId().trim() : null;
            String dateStr = att.getDate() != null ? att.getDate().trim() : null;

            if (empRef == null || dateStr == null) {
                summary.setInvalidRecords(summary.getInvalidRecords() + 1);
                summary.addIssue(new ImportRecordIssueDto(att.getId(), "ATT-REC", "INVALID", "Employee reference and attendance date are mandatory"));
                continue;
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr);
            } catch (Exception e) {
                summary.setInvalidRecords(summary.getInvalidRecords() + 1);
                summary.addIssue(new ImportRecordIssueDto(att.getId(), "ATT-REC", "INVALID", "Invalid date format: " + dateStr));
                continue;
            }

            // Resolve employee reference
            String resolvedCode = resolveEmpCode(empRef, availableEmpCodes, sourceEmpIdToCode);
            if (resolvedCode == null) {
                summary.setUnresolvedReferences(summary.getUnresolvedReferences() + 1);
                summary.addIssue(new ImportRecordIssueDto(att.getId(), dateStr, "UNRESOLVED_REF", "Referenced employee '" + empRef + "' does not exist in database or import payload"));
                continue;
            }

            // Check if record already exists in database
            Optional<Employee> empOpt = employeeRepository.findByEmployeeCode(resolvedCode);
            if (empOpt.isPresent() && attendanceRecordRepository.existsByEmployeeIdAndAttendanceDate(empOpt.get().getId(), date)) {
                summary.setExistingInDatabase(summary.getExistingInDatabase() + 1);
                summary.addIssue(new ImportRecordIssueDto(att.getId(), dateStr, "EXISTING", "Attendance record for employee " + resolvedCode + " on " + dateStr + " already exists in PostgreSQL"));
            } else {
                summary.setValidRecords(summary.getValidRecords() + 1);
                summary.setEligibleForImport(summary.getEligibleForImport() + 1);
            }
        }
        return summary;
    }

    private ModulePreviewSummaryDto previewLeaves(List<RawLeaveDto> leaves, Set<String> availableEmpCodes, Map<String, String> sourceEmpIdToCode) {
        ModulePreviewSummaryDto summary = new ModulePreviewSummaryDto("leaves");
        summary.setTotalSourceRecords(leaves.size());

        for (RawLeaveDto leave : leaves) {
            String empRef = leave.getEmployeeId() != null ? leave.getEmployeeId().trim() : null;
            String startStr = leave.getStartDate() != null ? leave.getStartDate().trim() : null;
            String endStr = leave.getEndDate() != null ? leave.getEndDate().trim() : null;

            if (empRef == null || startStr == null || endStr == null) {
                summary.setInvalidRecords(summary.getInvalidRecords() + 1);
                summary.addIssue(new ImportRecordIssueDto(leave.getId(), "LEAVE-REC", "INVALID", "Employee reference, start date, and end date are mandatory"));
                continue;
            }

            LocalDate start, end;
            try {
                start = LocalDate.parse(startStr);
                end = LocalDate.parse(endStr);
                if (end.isBefore(start)) {
                    summary.setInvalidRecords(summary.getInvalidRecords() + 1);
                    summary.addIssue(new ImportRecordIssueDto(leave.getId(), startStr, "INVALID", "End date cannot precede start date"));
                    continue;
                }
            } catch (Exception e) {
                summary.setInvalidRecords(summary.getInvalidRecords() + 1);
                summary.addIssue(new ImportRecordIssueDto(leave.getId(), startStr, "INVALID", "Invalid date format"));
                continue;
            }

            String resolvedCode = resolveEmpCode(empRef, availableEmpCodes, sourceEmpIdToCode);
            if (resolvedCode == null) {
                summary.setUnresolvedReferences(summary.getUnresolvedReferences() + 1);
                summary.addIssue(new ImportRecordIssueDto(leave.getId(), startStr, "UNRESOLVED_REF", "Referenced employee '" + empRef + "' does not exist in database or import payload"));
                continue;
            }

            Optional<Employee> empOpt = employeeRepository.findByEmployeeCode(resolvedCode);
            if (empOpt.isPresent()) {
                List<LeaveRequest> existing = leaveRequestRepository.findConflictingRequests(empOpt.get().getId(), start, end, null);
                if (!existing.isEmpty()) {
                    summary.setExistingInDatabase(summary.getExistingInDatabase() + 1);
                    summary.addIssue(new ImportRecordIssueDto(leave.getId(), startStr, "EXISTING", "Employee " + resolvedCode + " already has a leave request overlapping " + startStr + " to " + endStr));
                    continue;
                }
            }

            summary.setValidRecords(summary.getValidRecords() + 1);
            summary.setEligibleForImport(summary.getEligibleForImport() + 1);
        }
        return summary;
    }

    private String resolveEmpCode(String empRef, Set<String> availableEmpCodes, Map<String, String> sourceEmpIdToCode) {
        if (empRef == null) return null;
        String upper = empRef.trim().toUpperCase();
        if (availableEmpCodes.contains(upper)) {
            return upper;
        }
        if (sourceEmpIdToCode.containsKey(empRef.trim())) {
            return sourceEmpIdToCode.get(empRef.trim());
        }
        // Try fallback parsing like "emp-1001" -> "EMP-1001"
        if (upper.startsWith("EMP-")) {
            if (availableEmpCodes.contains(upper)) return upper;
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultDto executeImport(ImportExecuteRequestDto request, Authentication auth) {
        validateAdminAuth(auth);
        String adminEmail = auth.getName();

        if (request == null || !Boolean.TRUE.equals(request.getConfirmBackupTaken())) {
            throw new BusinessRuleViolationException("Import execution requires explicit confirmation that a database backup has been taken before proceeding.");
        }

        ImportPayloadDto payload = request.getPayload();
        if (payload == null) {
            payload = new ImportPayloadDto();
        }

        UUID jobId = UUID.randomUUID();
        ImportJob job = new ImportJob("EXECUTE", "IN_PROGRESS", adminEmail, request.getConflictStrategy() != null ? request.getConflictStrategy() : "SKIP_EXISTING");
        job.setId(jobId);
        job = importJobRepository.save(job);

        log.info("Starting transactional data import job [{}] initiated by [{}]", jobId, adminEmail);

        ImportResultDto result = new ImportResultDto();
        result.setJobId(jobId);
        result.setExecutedBy(adminEmail);
        result.setExecutedAt(Instant.now().toString());

        Map<String, UUID> sourceDeptToDbId = new HashMap<>();
        Map<String, UUID> sourceEmpToDbId = new HashMap<>();

        // 1. Process Departments
        int deptsImported = 0, deptsSkipped = 0, deptsFailed = 0;
        for (RawDepartmentDto rawDept : payload.getDepartments()) {
            try {
                String code = rawDept.getDepartmentId() != null ? rawDept.getDepartmentId().trim() : null;
                String name = rawDept.getName() != null ? rawDept.getName().trim() : null;

                if (code == null || name == null) {
                    deptsFailed++;
                    continue;
                }

                Optional<Department> existing = departmentRepository.findByDepartmentCode(code)
                        .or(() -> departmentRepository.findByNameIgnoreCase(name));

                if (existing.isPresent()) {
                    Department d = existing.get();
                    if (rawDept.getId() != null) sourceDeptToDbId.put(rawDept.getId(), d.getId());
                    sourceDeptToDbId.put(code, d.getId());
                    sourceDeptToDbId.put(name.toLowerCase(), d.getId());
                    deptsSkipped++;
                    recordManifest(job, "DEPARTMENT", rawDept.getId() != null ? rawDept.getId() : code, d.getId(), "SKIPPED", "Department already present in database");
                } else {
                    Department newDept = new Department(code, name, rawDept.getDescription());
                    if (rawDept.getStatus() != null && rawDept.getStatus().equalsIgnoreCase("Inactive")) {
                        newDept.setStatus("Inactive");
                    }
                    newDept = departmentRepository.save(newDept);
                    if (rawDept.getId() != null) sourceDeptToDbId.put(rawDept.getId(), newDept.getId());
                    sourceDeptToDbId.put(code, newDept.getId());
                    sourceDeptToDbId.put(name.toLowerCase(), newDept.getId());
                    deptsImported++;
                    recordManifest(job, "DEPARTMENT", rawDept.getId() != null ? rawDept.getId() : code, newDept.getId(), "CREATED", "Imported new department");
                }
            } catch (Exception e) {
                deptsFailed++;
                result.log("Failed to process department [" + rawDept.getDepartmentId() + "]: " + e.getMessage());
            }
        }

        // 2. Process Employees
        int empsImported = 0, empsSkipped = 0, empsFailed = 0;
        for (RawEmployeeDto rawEmp : payload.getEmployees()) {
            try {
                String code = rawEmp.getEmployeeId() != null ? rawEmp.getEmployeeId().trim() : null;
                String email = rawEmp.getEmail() != null ? rawEmp.getEmail().trim().toLowerCase() : null;

                if (code == null || email == null) {
                    empsFailed++;
                    continue;
                }

                Optional<Employee> existing = employeeRepository.findByEmployeeCode(code)
                        .or(() -> employeeRepository.findByEmailIgnoreCase(email));

                if (existing.isPresent()) {
                    Employee e = existing.get();
                    if (rawEmp.getId() != null) sourceEmpToDbId.put(rawEmp.getId(), e.getId());
                    sourceEmpToDbId.put(code, e.getId());
                    empsSkipped++;
                    recordManifest(job, "EMPLOYEE", rawEmp.getId() != null ? rawEmp.getId() : code, e.getId(), "SKIPPED", "Employee already present in database");
                } else {
                    // Resolve department
                    UUID deptId = null;
                    if (rawEmp.getDepartment() != null) {
                        String deptRef = rawEmp.getDepartment().trim();
                        deptId = sourceDeptToDbId.get(deptRef);
                        if (deptId == null) deptId = sourceDeptToDbId.get(deptRef.toLowerCase());
                    }
                    if (deptId == null) {
                        Department fallback = departmentRepository.findAll().stream().findFirst().orElse(null);
                        if (fallback != null) deptId = fallback.getId();
                    }

                    if (deptId == null) {
                        empsFailed++;
                        result.log("Cannot import employee [" + code + "]: no valid department found");
                        continue;
                    }

                    Department targetDept = departmentRepository.findById(deptId).orElseThrow();
                    LocalDate joinDate = LocalDate.now();
                    if (rawEmp.getJoiningDate() != null) {
                        try { joinDate = LocalDate.parse(rawEmp.getJoiningDate().trim()); } catch (Exception ignored) {}
                    }

                    Employee newEmp = new Employee(
                            code,
                            rawEmp.getFirstName() != null ? rawEmp.getFirstName().trim() : "Unknown",
                            rawEmp.getLastName() != null ? rawEmp.getLastName().trim() : "Employee",
                            email,
                            rawEmp.getDesignation() != null ? rawEmp.getDesignation().trim() : "Team Member",
                            targetDept,
                            joinDate
                    );

                    if (rawEmp.getPhone() != null) newEmp.setPhone(rawEmp.getPhone().trim());
                    if (rawEmp.getAddress() != null) newEmp.setAddress(rawEmp.getAddress().trim());

                    if (rawEmp.getEmploymentStatus() != null) {
                        try { newEmp.setEmploymentStatus(EmploymentStatus.fromString(rawEmp.getEmploymentStatus())); } catch (Exception ignored) {}
                    }
                    if (rawEmp.getEmploymentType() != null) {
                        try { newEmp.setEmploymentType(EmploymentType.fromString(rawEmp.getEmploymentType())); } catch (Exception ignored) {}
                    }

                    newEmp = employeeRepository.save(newEmp);
                    if (rawEmp.getId() != null) sourceEmpToDbId.put(rawEmp.getId(), newEmp.getId());
                    sourceEmpToDbId.put(code, newEmp.getId());
                    empsImported++;
                    recordManifest(job, "EMPLOYEE", rawEmp.getId() != null ? rawEmp.getId() : code, newEmp.getId(), "CREATED", "Imported new employee");
                }
            } catch (Exception e) {
                empsFailed++;
                result.log("Failed to process employee [" + rawEmp.getEmployeeId() + "]: " + e.getMessage());
            }
        }

        // 3. Update Department Managers
        for (RawDepartmentDto rawDept : payload.getDepartments()) {
            if (rawDept.getManagerEmployeeId() != null) {
                UUID mgrEmpId = sourceEmpToDbId.get(rawDept.getManagerEmployeeId().trim());
                if (mgrEmpId != null) {
                    UUID deptId = sourceDeptToDbId.get(rawDept.getDepartmentId() != null ? rawDept.getDepartmentId().trim() : null);
                    if (deptId != null) {
                        Department d = departmentRepository.findById(deptId).orElse(null);
                        Employee mgr = employeeRepository.findById(mgrEmpId).orElse(null);
                        if (d != null && mgr != null && d.getManager() == null) {
                            d.setManager(mgr);
                            departmentRepository.save(d);
                        }
                    }
                }
            }
        }

        // 4. Process Attendance Records
        int attImported = 0, attSkipped = 0, attFailed = 0;
        for (RawAttendanceDto rawAtt : payload.getAttendance()) {
            try {
                String empRef = rawAtt.getEmployeeId() != null ? rawAtt.getEmployeeId().trim() : null;
                String dateStr = rawAtt.getDate() != null ? rawAtt.getDate().trim() : null;

                if (empRef == null || dateStr == null) {
                    attFailed++;
                    continue;
                }

                UUID empId = sourceEmpToDbId.get(empRef);
                if (empId == null && empRef.startsWith("emp-")) {
                    empId = sourceEmpToDbId.get(empRef.replace("emp-", "EMP-"));
                }
                if (empId == null) {
                    // Try parsing as raw UUID if already a UUID
                    try { empId = UUID.fromString(empRef); } catch (Exception ignored) {}
                }

                if (empId == null) {
                    attFailed++;
                    continue;
                }

                LocalDate attDate = LocalDate.parse(dateStr);
                Optional<Employee> empOpt = employeeRepository.findById(empId);
                if (empOpt.isEmpty()) {
                    attFailed++;
                    continue;
                }

                Employee emp = empOpt.get();
                if (attendanceRecordRepository.existsByEmployeeIdAndAttendanceDate(empId, attDate)) {
                    attSkipped++;
                    recordManifest(job, "ATTENDANCE", rawAtt.getId() != null ? rawAtt.getId() : empRef + "_" + dateStr, empId, "SKIPPED", "Attendance record already present for date " + dateStr);
                } else {
                    AttendanceStatus status = AttendanceStatus.PRESENT;
                    if (rawAtt.getStatus() != null) {
                        try { status = AttendanceStatus.fromString(rawAtt.getStatus()); } catch (Exception ignored) {}
                    }

                    LocalTime checkIn = null;
                    if (rawAtt.getCheckIn() != null && !rawAtt.getCheckIn().trim().isEmpty()) {
                        try {
                            String cIn = rawAtt.getCheckIn().trim();
                            if (cIn.length() == 5) checkIn = LocalTime.parse(cIn);
                            else if (cIn.length() > 5) checkIn = LocalTime.parse(cIn.substring(0, 5));
                        } catch (Exception ignored) {}
                    }
                    if (checkIn == null && status != AttendanceStatus.ABSENT) {
                        checkIn = LocalTime.of(9, 0);
                    }

                    LocalTime checkOut = null;
                    if (rawAtt.getCheckOut() != null && !rawAtt.getCheckOut().trim().isEmpty()) {
                        try {
                            String cOut = rawAtt.getCheckOut().trim();
                            if (cOut.length() == 5) checkOut = LocalTime.parse(cOut);
                            else if (cOut.length() > 5) checkOut = LocalTime.parse(cOut.substring(0, 5));
                        } catch (Exception ignored) {}
                    }

                    AttendanceRecord record = new AttendanceRecord(emp, attDate, checkIn, status);
                    record.setCheckOutTime(checkOut);
                    record.setNotes(rawAtt.getNotes());
                    record = attendanceRecordRepository.save(record);

                    attImported++;
                    recordManifest(job, "ATTENDANCE", rawAtt.getId() != null ? rawAtt.getId() : empRef + "_" + dateStr, record.getId(), "CREATED", "Imported historical attendance record");
                }
            } catch (Exception e) {
                attFailed++;
                result.log("Failed to process attendance [" + rawAtt.getId() + "]: " + e.getMessage());
            }
        }

        // 5. Process Leave Requests
        int leavesImported = 0, leavesSkipped = 0, leavesFailed = 0;
        for (RawLeaveDto rawLeave : payload.getLeaves()) {
            try {
                String empRef = rawLeave.getEmployeeId() != null ? rawLeave.getEmployeeId().trim() : null;
                String startStr = rawLeave.getStartDate() != null ? rawLeave.getStartDate().trim() : null;
                String endStr = rawLeave.getEndDate() != null ? rawLeave.getEndDate().trim() : null;

                if (empRef == null || startStr == null || endStr == null) {
                    leavesFailed++;
                    continue;
                }

                UUID empId = sourceEmpToDbId.get(empRef);
                if (empId == null && empRef.startsWith("emp-")) {
                    empId = sourceEmpToDbId.get(empRef.replace("emp-", "EMP-"));
                }
                if (empId == null) {
                    try { empId = UUID.fromString(empRef); } catch (Exception ignored) {}
                }

                if (empId == null) {
                    leavesFailed++;
                    continue;
                }

                Optional<Employee> empOpt = employeeRepository.findById(empId);
                if (empOpt.isEmpty()) {
                    leavesFailed++;
                    continue;
                }

                Employee emp = empOpt.get();
                LocalDate start = LocalDate.parse(startStr);
                LocalDate end = LocalDate.parse(endStr);

                List<LeaveRequest> conflicting = leaveRequestRepository.findConflictingRequests(empId, start, end, null);
                if (!conflicting.isEmpty()) {
                    leavesSkipped++;
                    recordManifest(job, "LEAVE", rawLeave.getId() != null ? rawLeave.getId() : empRef + "_" + startStr, empId, "SKIPPED", "Leave request already present or overlapping");
                } else {
                    LeaveType type = LeaveType.ANNUAL;
                    if (rawLeave.getLeaveType() != null) {
                        try { type = LeaveType.fromString(rawLeave.getLeaveType()); } catch (Exception ignored) {}
                    }

                    LeaveStatus status = LeaveStatus.PENDING;
                    if (rawLeave.getStatus() != null) {
                        try { status = LeaveStatus.valueOf(rawLeave.getStatus().trim().toUpperCase()); } catch (Exception ignored) {}
                    }

                    int days = rawLeave.getRequestedDays() != null && rawLeave.getRequestedDays() > 0
                            ? rawLeave.getRequestedDays()
                            : (int) ChronoUnit.DAYS.between(start, end) + 1;

                    LeaveRequest leaveReq = new LeaveRequest(emp, type, start, end, days, rawLeave.getReason() != null ? rawLeave.getReason() : "Historical leave");
                    leaveReq.setStatus(status);

                    if (rawLeave.getReviewerComment() != null) {
                        leaveReq.setReviewComment(rawLeave.getReviewerComment());
                    }
                    if (status != LeaveStatus.PENDING) {
                        leaveReq.setReviewedAt(Instant.now());
                    }

                    leaveReq = leaveRequestRepository.save(leaveReq);
                    leavesImported++;
                    recordManifest(job, "LEAVE", rawLeave.getId() != null ? rawLeave.getId() : empRef + "_" + startStr, leaveReq.getId(), "CREATED", "Imported historical leave request");
                }
            } catch (Exception e) {
                leavesFailed++;
                result.log("Failed to process leave request [" + rawLeave.getId() + "]: " + e.getMessage());
            }
        }

        // Wrap up execution
        result.setStatus((deptsFailed + empsFailed + attFailed + leavesFailed) == 0 ? "SUCCESS" : "PARTIAL_SUCCESS");
        result.getImportedCounts().put("departments", deptsImported);
        result.getImportedCounts().put("employees", empsImported);
        result.getImportedCounts().put("attendance", attImported);
        result.getImportedCounts().put("leaves", leavesImported);

        result.getSkippedCounts().put("departments", deptsSkipped);
        result.getSkippedCounts().put("employees", empsSkipped);
        result.getSkippedCounts().put("attendance", attSkipped);
        result.getSkippedCounts().put("leaves", leavesSkipped);

        result.getFailedCounts().put("departments", deptsFailed);
        result.getFailedCounts().put("employees", empsFailed);
        result.getFailedCounts().put("attendance", attFailed);
        result.getFailedCounts().put("leaves", leavesFailed);

        job.setStatus(result.getStatus());
        job.setCompletedAt(Instant.now());
        try {
            job.setResultSummaryJson(objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException ignored) {}
        importJobRepository.save(job);

        log.info("Completed import job [{}]: Status=[{}], Imported=[Depts:{}, Emps:{}, Att:{}, Leaves:{}], Skipped=[Depts:{}, Emps:{}, Att:{}, Leaves:{}]",
                jobId, result.getStatus(), deptsImported, empsImported, attImported, leavesImported,
                deptsSkipped, empsSkipped, attSkipped, leavesSkipped);

        return result;
    }

    private void recordManifest(ImportJob job, String entityType, String sourceId, UUID targetId, String action, String details) {
        ImportManifest manifest = new ImportManifest(job, entityType, sourceId, targetId, action, details);
        importManifestRepository.save(manifest);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportJobDto getJobStatus(UUID jobId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job with ID " + jobId + " not found."));
        ImportJobDto dto = new ImportJobDto();
        dto.setId(job.getId());
        dto.setJobType(job.getJobType());
        dto.setStatus(job.getStatus());
        dto.setInitiatedBy(job.getInitiatedBy());
        dto.setConflictStrategy(job.getConflictStrategy());
        dto.setSourceCountsJson(job.getSourceCountsJson());
        dto.setResultSummaryJson(job.getResultSummaryJson());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setCompletedAt(job.getCompletedAt());
        return dto;
    }

    private void validateAdminAuth(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required to perform data migration operations.");
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Administrative privileges are required to access data migration utilities.");
        }
    }
}

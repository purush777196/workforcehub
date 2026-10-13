package com.workforcehub.repository;

import com.workforcehub.entity.AttendanceRecord;
import com.workforcehub.entity.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID>, JpaSpecificationExecutor<AttendanceRecord> {
    Optional<AttendanceRecord> findByEmployeeIdAndAttendanceDate(UUID employeeId, LocalDate attendanceDate);
    List<AttendanceRecord> findByAttendanceDate(LocalDate attendanceDate);
    List<AttendanceRecord> findByEmployeeId(UUID employeeId);
    boolean existsByEmployeeId(UUID employeeId);
    boolean existsByEmployeeIdAndAttendanceDate(UUID employeeId, LocalDate attendanceDate);
    long countByAttendanceDateAndStatus(LocalDate attendanceDate, AttendanceStatus status);
    long countByAttendanceDate(LocalDate attendanceDate);
}

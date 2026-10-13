package com.workforcehub.repository;

import com.workforcehub.entity.LeaveRequest;
import com.workforcehub.entity.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID>, JpaSpecificationExecutor<LeaveRequest> {

    List<LeaveRequest> findByEmployeeId(UUID employeeId);

    List<LeaveRequest> findByStatus(LeaveStatus status);

    long countByStatus(LeaveStatus status);

    boolean existsByEmployeeId(UUID employeeId);

    boolean existsByReviewedById(UUID reviewedById);

    @Query("SELECT r FROM LeaveRequest r WHERE r.employee.id = :employeeId AND r.status = :status AND r.startDate <= :endDate AND r.endDate >= :startDate")
    List<LeaveRequest> findOverlappingRequests(
        @Param("employeeId") UUID employeeId, 
        @Param("status") LeaveStatus status, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT r FROM LeaveRequest r WHERE r.employee.id = :employeeId AND r.status IN ('PENDING', 'APPROVED') AND r.startDate <= :endDate AND r.endDate >= :startDate AND (:excludeId IS NULL OR r.id != :excludeId)")
    List<LeaveRequest> findConflictingRequests(
        @Param("employeeId") UUID employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludeId") UUID excludeId
    );

    @Query("SELECT r FROM LeaveRequest r WHERE r.employee.id = :employeeId AND r.status = 'APPROVED' AND r.startDate <= :date AND r.endDate >= :date")
    List<LeaveRequest> findApprovedLeavesOnDate(
        @Param("employeeId") UUID employeeId,
        @Param("date") LocalDate date
    );

    @Query("SELECT COUNT(DISTINCT r.employee.id) FROM LeaveRequest r WHERE r.status = 'APPROVED' AND r.startDate <= :date AND r.endDate >= :date")
    long countApprovedEmployeesOnDate(@Param("date") LocalDate date);

    @Query("SELECT r FROM LeaveRequest r WHERE r.employee.id = :employeeId AND r.status = 'APPROVED' AND r.startDate >= :startOfYear AND r.startDate <= :endOfYear")
    List<LeaveRequest> findApprovedLeavesInDateRange(
        @Param("employeeId") UUID employeeId,
        @Param("startOfYear") LocalDate startOfYear,
        @Param("endOfYear") LocalDate endOfYear
    );
}

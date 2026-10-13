package com.workforcehub.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.workforcehub.entity.LeaveRequest;
import com.workforcehub.entity.enums.LeaveStatus;
import com.workforcehub.entity.enums.LeaveType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data transfer object for Leave Requests.
 * Provides backwards-compatible field aliases (requestedDays, submittedAt, reviewedBy, reviewerComment)
 * for the React client.
 */
public class LeaveRequestDto {

    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private String employeeCode;
    private UUID departmentId;
    private String departmentName;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int daysCount;
    private String reason;
    private LeaveStatus status;
    private UUID reviewedById;
    private String reviewedByName;
    private String reviewComment;
    private Instant reviewedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public LeaveRequestDto() {
    }

    public static LeaveRequestDto fromEntity(LeaveRequest entity) {
        if (entity == null) return null;
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setId(entity.getId());
        if (entity.getEmployee() != null) {
            dto.setEmployeeId(entity.getEmployee().getId());
            dto.setEmployeeName(entity.getEmployee().getFullName());
            dto.setEmployeeCode(entity.getEmployee().getEmployeeCode());
            if (entity.getEmployee().getDepartment() != null) {
                dto.setDepartmentId(entity.getEmployee().getDepartment().getId());
                dto.setDepartmentName(entity.getEmployee().getDepartment().getName());
            }
        }
        dto.setLeaveType(entity.getLeaveType());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setDaysCount(entity.getDaysCount());
        dto.setReason(entity.getReason());
        dto.setStatus(entity.getStatus());
        if (entity.getReviewedBy() != null) {
            dto.setReviewedById(entity.getReviewedBy().getId());
            dto.setReviewedByName(entity.getReviewedBy().getFullName());
        }
        dto.setReviewComment(entity.getReviewComment());
        dto.setReviewedAt(entity.getReviewedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    // Frontend compatibility getters
    @JsonGetter("requestedDays")
    public int getRequestedDays() {
        return daysCount;
    }

    @JsonGetter("submittedAt")
    public Instant getSubmittedAt() {
        return createdAt;
    }

    @JsonGetter("reviewedBy")
    public String getReviewedBy() {
        return reviewedByName;
    }

    @JsonGetter("reviewerComment")
    public String getReviewerComment() {
        return reviewComment;
    }

    public String getRejectionReason() {
        return reviewComment;
    }

    public void setRejectionReason(String rejectionReason) {
        this.reviewComment = rejectionReason;
    }

    public String getReviewerName() {
        return reviewedByName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewedByName = reviewerName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(UUID departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getDaysCount() {
        return daysCount;
    }

    public void setDaysCount(int daysCount) {
        this.daysCount = daysCount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public UUID getReviewedById() {
        return reviewedById;
    }

    public void setReviewedById(UUID reviewedById) {
        this.reviewedById = reviewedById;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public void setReviewedByName(String reviewedByName) {
        this.reviewedByName = reviewedByName;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

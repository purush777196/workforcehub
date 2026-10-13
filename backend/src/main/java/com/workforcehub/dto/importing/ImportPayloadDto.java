package com.workforcehub.dto.importing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportPayloadDto {
    private List<RawDepartmentDto> departments = new ArrayList<>();
    private List<RawEmployeeDto> employees = new ArrayList<>();
    private List<RawAttendanceDto> attendance = new ArrayList<>();
    private List<RawLeaveDto> leaves = new ArrayList<>();

    public ImportPayloadDto() {}

    public List<RawDepartmentDto> getDepartments() { return departments; }
    public void setDepartments(List<RawDepartmentDto> departments) {
        this.departments = departments != null ? departments : new ArrayList<>();
    }

    public List<RawEmployeeDto> getEmployees() { return employees; }
    public void setEmployees(List<RawEmployeeDto> employees) {
        this.employees = employees != null ? employees : new ArrayList<>();
    }

    public List<RawAttendanceDto> getAttendance() { return attendance; }
    public void setAttendance(List<RawAttendanceDto> attendance) {
        this.attendance = attendance != null ? attendance : new ArrayList<>();
    }

    public List<RawLeaveDto> getLeaves() { return leaves; }
    public void setLeaves(List<RawLeaveDto> leaves) {
        this.leaves = leaves != null ? leaves : new ArrayList<>();
    }
}

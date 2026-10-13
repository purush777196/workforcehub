package com.workforcehub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.workforcehub.config.CorsConfig;
import com.workforcehub.dto.*;
import com.workforcehub.entity.enums.AttendanceStatus;
import com.workforcehub.exception.BusinessRuleViolationException;
import com.workforcehub.exception.DuplicateResourceException;
import com.workforcehub.exception.GlobalExceptionHandler;
import com.workforcehub.exception.ResourceNotFoundException;
import com.workforcehub.security.SecurityConfig;
import com.workforcehub.service.AttendanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttendanceController.class)
@Import({SecurityConfig.class, CorsConfig.class, GlobalExceptionHandler.class})
class AttendanceControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private AttendanceRecordDto sampleDto;
    private UUID sampleId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        sampleDto = new AttendanceRecordDto();
        sampleDto.setId(sampleId);
        sampleDto.setEmployeeId(employeeId);
        sampleDto.setEmployeeName("Elena Rostova");
        sampleDto.setEmployeeCode("EMP-1001");
        sampleDto.setDepartmentName("Engineering");
        sampleDto.setAttendanceDate(LocalDate.now());
        sampleDto.setCheckInTime(LocalTime.of(8, 45));
        sampleDto.setCheckOutTime(LocalTime.of(17, 15));
        sampleDto.setStatus(AttendanceStatus.PRESENT);
        sampleDto.setMinutesWorked(510L);
        sampleDto.setDurationDisplay("8h 30m");
        sampleDto.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Anonymous access to GET /api/attendance returns 401 Unauthorized")
    void testAnonymousGetAttendanceReturns401() throws Exception {
        mockMvc.perform(get("/api/attendance"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("Authentication is required")));
    }

    @Test
    @DisplayName("Anonymous check-in returns 401 Unauthorized")
    void testAnonymousCheckInReturns401() throws Exception {
        mockMvc.perform(post("/api/attendance/check-in").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    @DisplayName("ADMIN can list attendance records (200 OK)")
    void testAdminCanListAttendance() throws Exception {
        PagedResponse<AttendanceRecordDto> paged = new PagedResponse<>(
                List.of(sampleDto), 0, 10, 1, 1, true, true
        );
        given(attendanceService.getAttendanceRecords(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any(), any()))
                .willReturn(paged);

        mockMvc.perform(get("/api/attendance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].employeeName").value("Elena Rostova"));
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE can access GET /api/attendance (service restricts to own records)")
    void testEmployeeCanAccessOwnAttendanceList() throws Exception {
        PagedResponse<AttendanceRecordDto> paged = new PagedResponse<>(
                List.of(sampleDto), 0, 10, 1, 1, true, true
        );
        given(attendanceService.getAttendanceRecords(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any(), any()))
                .willReturn(paged);

        mockMvc.perform(get("/api/attendance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].employeeCode").value("EMP-1001"));
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE accessing another employee's record by ID returns 403 Forbidden")
    void testEmployeeAccessingOtherRecordReturns403() throws Exception {
        given(attendanceService.getAttendanceById(eq(sampleId), any()))
                .willThrow(new AccessDeniedException("You do not have permission to access another employee's attendance record."));

        mockMvc.perform(get("/api/attendance/{id}", sampleId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("permission")));
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE can retrieve their own attendance record by ID (200 OK)")
    void testEmployeeCanAccessOwnRecordById() throws Exception {
        given(attendanceService.getAttendanceById(eq(sampleId), any())).willReturn(sampleDto);

        mockMvc.perform(get("/api/attendance/{id}", sampleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeName").value("Elena Rostova"));
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE can check in with valid CSRF token (201 Created)")
    void testEmployeeCheckInWithCsrf() throws Exception {
        given(attendanceService.checkIn(any(), any())).willReturn(sampleDto);

        AttendanceCheckInRequest request = new AttendanceCheckInRequest("Starting shift");

        mockMvc.perform(post("/api/attendance/check-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PRESENT"));
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("Check-in without CSRF token is rejected (403 Forbidden)")
    void testCheckInWithoutCsrfRejected() throws Exception {
        AttendanceCheckInRequest request = new AttendanceCheckInRequest("Starting shift");

        mockMvc.perform(post("/api/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE can check out with valid CSRF token (200 OK)")
    void testEmployeeCheckOutWithCsrf() throws Exception {
        given(attendanceService.checkOut(any(), any())).willReturn(sampleDto);

        AttendanceCheckOutRequest request = new AttendanceCheckOutRequest("Shift ended");

        mockMvc.perform(post("/api/attendance/check-out")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.durationDisplay").value("8h 30m"));
    }

    @Test
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    @DisplayName("ADMIN can create manual attendance record with CSRF (201 Created)")
    void testAdminCreateManualAttendance() throws Exception {
        given(attendanceService.createManualAttendance(any(), any())).willReturn(sampleDto);

        AttendanceCreateRequest request = new AttendanceCreateRequest(
                employeeId,
                LocalDate.now(),
                LocalTime.of(9, 0),
                AttendanceStatus.PRESENT
        );

        mockMvc.perform(post("/api/attendance")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE attempting manual attendance creation returns 403 Forbidden")
    void testEmployeeManualCreationForbidden() throws Exception {
        AttendanceCreateRequest request = new AttendanceCreateRequest(
                employeeId,
                LocalDate.now(),
                LocalTime.of(9, 0),
                AttendanceStatus.PRESENT
        );

        mockMvc.perform(post("/api/attendance")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    @DisplayName("ADMIN can update attendance record with CSRF (200 OK)")
    void testAdminUpdateAttendance() throws Exception {
        given(attendanceService.updateAttendanceRecord(eq(sampleId), any(), any())).willReturn(sampleDto);

        AttendanceUpdateRequest request = new AttendanceUpdateRequest(
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                AttendanceStatus.PRESENT,
                "Admin verified"
        );

        mockMvc.perform(put("/api/attendance/{id}", sampleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE attempting attendance update returns 403 Forbidden")
    void testEmployeeUpdateAttendanceForbidden() throws Exception {
        AttendanceUpdateRequest request = new AttendanceUpdateRequest(
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                AttendanceStatus.PRESENT,
                "Override attempt"
        );

        mockMvc.perform(put("/api/attendance/{id}", sampleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("Authenticated user can retrieve attendance summary metrics (200 OK)")
    void testGetAttendanceSummary() throws Exception {
        AttendanceSummaryDto summary = new AttendanceSummaryDto(
                LocalDate.now(), 15, 16, 13, 10, 2, 1, 0, 2, 100
        );
        given(attendanceService.getAttendanceSummary(any(), any())).willReturn(summary);

        mockMvc.perform(get("/api/attendance/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attendanceRate").value(100));
    }
}

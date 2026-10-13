package com.workforcehub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.workforcehub.config.CorsConfig;
import com.workforcehub.dto.*;
import com.workforcehub.entity.enums.LeaveStatus;
import com.workforcehub.entity.enums.LeaveType;
import com.workforcehub.exception.GlobalExceptionHandler;
import com.workforcehub.security.SecurityConfig;
import com.workforcehub.service.LeaveService;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveController.class)
@Import({SecurityConfig.class, CorsConfig.class, GlobalExceptionHandler.class})
class LeaveControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveService leaveService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private LeaveRequestDto sampleDto;
    private UUID sampleId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        sampleDto = new LeaveRequestDto();
        sampleDto.setId(sampleId);
        sampleDto.setEmployeeId(employeeId);
        sampleDto.setEmployeeName("Elena Rostova");
        sampleDto.setEmployeeCode("EMP-1001");
        sampleDto.setDepartmentName("Engineering");
        sampleDto.setLeaveType(LeaveType.ANNUAL);
        sampleDto.setStartDate(LocalDate.of(2026, 11, 2));
        sampleDto.setEndDate(LocalDate.of(2026, 11, 6));
        sampleDto.setDaysCount(5);
        sampleDto.setStatus(LeaveStatus.PENDING);
        sampleDto.setReason("Annual family holiday");
        sampleDto.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Anonymous access to GET /api/leaves returns 401 Unauthorized")
    void testAnonymousGetLeavesReturns401() throws Exception {
        mockMvc.perform(get("/api/leaves"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("Authentication is required")));
    }

    @Test
    @DisplayName("Anonymous access to POST /api/leaves returns 401 Unauthorized")
    void testAnonymousApplyLeaveReturns401() throws Exception {
        mockMvc.perform(post("/api/leaves").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    @DisplayName("ADMIN can list leave requests (200 OK)")
    void testAdminCanListLeaves() throws Exception {
        PagedResponse<LeaveRequestDto> paged = new PagedResponse<>(
                List.of(sampleDto), 0, 10, 1, 1, true, true
        );
        given(leaveService.getLeaveRequests(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any(), any()))
                .willReturn(paged);

        mockMvc.perform(get("/api/leaves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].employeeName").value("Elena Rostova"));
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE can access GET /api/leaves (service restricts to own records)")
    void testEmployeeCanAccessOwnLeaveList() throws Exception {
        PagedResponse<LeaveRequestDto> paged = new PagedResponse<>(
                List.of(sampleDto), 0, 10, 1, 1, true, true
        );
        given(leaveService.getLeaveRequests(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any(), any()))
                .willReturn(paged);

        mockMvc.perform(get("/api/leaves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].employeeCode").value("EMP-1001"));
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE accessing another employee's leave by ID returns 403 Forbidden")
    void testEmployeeAccessingOtherLeaveReturns403() throws Exception {
        given(leaveService.getLeaveRequestById(eq(sampleId), any()))
                .willThrow(new AccessDeniedException("You do not have permission to access another employee's leave request."));

        mockMvc.perform(get("/api/leaves/{id}", sampleId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("permission")));
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE can retrieve their own leave request by ID (200 OK)")
    void testEmployeeCanAccessOwnLeaveById() throws Exception {
        given(leaveService.getLeaveRequestById(eq(sampleId), any())).willReturn(sampleDto);

        mockMvc.perform(get("/api/leaves/{id}", sampleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeName").value("Elena Rostova"));
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE can apply for leave with valid CSRF token (201 Created)")
    void testEmployeeApplyLeaveWithCsrf() throws Exception {
        given(leaveService.applyLeave(any(), any())).willReturn(sampleDto);

        LeaveApplyRequest request = new LeaveApplyRequest();
        request.setLeaveType(LeaveType.ANNUAL);
        request.setStartDate(LocalDate.of(2026, 11, 2));
        request.setEndDate(LocalDate.of(2026, 11, 6));
        request.setReason("Annual family holiday");

        mockMvc.perform(post("/api/leaves")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("Leave application without CSRF token is rejected (403 Forbidden)")
    void testApplyLeaveWithoutCsrfRejected() throws Exception {
        LeaveApplyRequest request = new LeaveApplyRequest();
        request.setLeaveType(LeaveType.ANNUAL);
        request.setStartDate(LocalDate.of(2026, 11, 2));
        request.setEndDate(LocalDate.of(2026, 11, 6));
        request.setReason("Annual family holiday");

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE attempting to approve leave returns 403 Forbidden")
    void testEmployeeCannotApproveLeave() throws Exception {
        LeaveReviewRequest review = new LeaveReviewRequest("Employee trying to self approve");

        mockMvc.perform(post("/api/leaves/{id}/approve", sampleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE attempting to reject leave returns 403 Forbidden")
    void testEmployeeCannotRejectLeave() throws Exception {
        LeaveReviewRequest review = new LeaveReviewRequest("Employee trying to reject");

        mockMvc.perform(post("/api/leaves/{id}/reject", sampleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE attempting to inspect other employee balance returns 403 Forbidden")
    void testEmployeeCannotInspectOtherBalance() throws Exception {
        UUID otherId = UUID.randomUUID();
        mockMvc.perform(get("/api/leaves/balance/{employeeId}", otherId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "elena@workforcehub.io", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE can retrieve own balance /api/leaves/balance/me (200 OK)")
    void testEmployeeCanRetrieveOwnBalance() throws Exception {
        LeaveBalancesDto balances = new LeaveBalancesDto();
        balances.setEmployeeId(employeeId);
        balances.setYear(2026);
        balances.setAnnual(new LeaveBalanceItemDto(20, 5, 15));
        balances.setSick(new LeaveBalanceItemDto(12, 0, 12));
        balances.setPersonal(new LeaveBalanceItemDto(5, 0, 5));
        balances.setUnpaid(new LeaveBalanceItemDto(30, 0, 30));

        given(leaveService.getEmployeeBalances(isNull(), any(), any())).willReturn(balances);

        mockMvc.perform(get("/api/leaves/balance/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.annual.remaining").value(15));
    }

    @Test
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    @DisplayName("ADMIN can approve leave with valid CSRF token (200 OK)")
    void testAdminCanApproveLeave() throws Exception {
        sampleDto.setStatus(LeaveStatus.APPROVED);
        sampleDto.setReviewedByName("Sarah Admin");
        sampleDto.setRejectionReason("Approved by management");

        given(leaveService.approveLeave(eq(sampleId), any(), any())).willReturn(sampleDto);

        LeaveReviewRequest review = new LeaveReviewRequest("Approved by management");

        mockMvc.perform(post("/api/leaves/{id}/approve", sampleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewerName").value("Sarah Admin"));
    }

    @Test
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    @DisplayName("ADMIN can reject leave with valid CSRF token (200 OK)")
    void testAdminCanRejectLeave() throws Exception {
        sampleDto.setStatus(LeaveStatus.REJECTED);
        sampleDto.setReviewedByName("Sarah Admin");
        sampleDto.setRejectionReason("Staffing shortage");

        given(leaveService.rejectLeave(eq(sampleId), any(), any())).willReturn(sampleDto);

        LeaveReviewRequest review = new LeaveReviewRequest("Staffing shortage");

        mockMvc.perform(post("/api/leaves/{id}/reject", sampleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    @DisplayName("ADMIN can inspect employee balance /api/leaves/balance/{employeeId} (200 OK)")
    void testAdminCanInspectEmployeeBalance() throws Exception {
        LeaveBalancesDto balances = new LeaveBalancesDto();
        balances.setEmployeeId(employeeId);
        balances.setYear(2026);
        balances.setAnnual(new LeaveBalanceItemDto(20, 8, 12));
        balances.setSick(new LeaveBalanceItemDto(12, 1, 11));
        balances.setPersonal(new LeaveBalanceItemDto(5, 1, 4));
        balances.setUnpaid(new LeaveBalanceItemDto(30, 0, 30));

        given(leaveService.getEmployeeBalances(eq(employeeId), any(), any())).willReturn(balances);

        mockMvc.perform(get("/api/leaves/balance/{employeeId}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.annual.used").value(8));
    }
}

package com.workforcehub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.workforcehub.config.CorsConfig;
import com.workforcehub.dto.EmployeeCreateRequest;
import com.workforcehub.dto.EmployeeDto;
import com.workforcehub.dto.EmployeeUpdateRequest;
import com.workforcehub.dto.PagedResponse;
import com.workforcehub.entity.enums.EmploymentStatus;
import com.workforcehub.entity.enums.EmploymentType;
import com.workforcehub.exception.DuplicateResourceException;
import com.workforcehub.exception.GlobalExceptionHandler;
import com.workforcehub.exception.ResourceNotFoundException;
import com.workforcehub.security.SecurityConfig;
import com.workforcehub.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
@Import({SecurityConfig.class, CorsConfig.class, GlobalExceptionHandler.class})
class EmployeeControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private EmployeeDto sampleDto;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleDto = new EmployeeDto();
        sampleDto.setId(sampleId);
        sampleDto.setEmployeeCode("EMP-1001");
        sampleDto.setFirstName("Sarah");
        sampleDto.setLastName("Jenkins");
        sampleDto.setEmail("sarah.jenkins@workforcehub.io");
        sampleDto.setJobTitle("Staff Software Engineer");
        sampleDto.setDepartmentName("Engineering");
        sampleDto.setEmploymentStatus(EmploymentStatus.ACTIVE);
        sampleDto.setEmploymentType(EmploymentType.FULL_TIME);
        sampleDto.setJoiningDate(LocalDate.of(2023, 3, 15));
    }

    @Test
    @DisplayName("GET /api/employees - Anonymous request returns 401 Unauthorized")
    void testAnonymousGetEmployeesReturns401() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("POST /api/employees - Anonymous request returns 401 Unauthorized")
    void testAnonymousPostEmployeesReturns401() throws Exception {
        EmployeeCreateRequest req = new EmployeeCreateRequest();
        req.setFirstName("Sarah");
        req.setLastName("Jenkins");
        req.setEmail("sarah.jenkins@workforcehub.io");
        req.setJobTitle("Engineer");
        req.setJoiningDate(LocalDate.now());

        mockMvc.perform(post("/api/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/employees - EMPLOYEE role receives 403 Forbidden")
    @WithMockUser(roles = "EMPLOYEE")
    void testEmployeeRoleGetEmployeesReturns403() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("POST /api/employees - EMPLOYEE role receives 403 Forbidden")
    @WithMockUser(roles = "EMPLOYEE")
    void testEmployeeRolePostEmployeesReturns403() throws Exception {
        EmployeeCreateRequest req = new EmployeeCreateRequest();
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setEmail("jane.doe@workforcehub.io");
        req.setJobTitle("Designer");
        req.setJoiningDate(LocalDate.now());

        mockMvc.perform(post("/api/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/employees - ADMIN can retrieve paginated employees")
    @WithMockUser(roles = "ADMIN")
    void testAdminGetEmployeesSuccess() throws Exception {
        PagedResponse<EmployeeDto> paged = new PagedResponse<>(
                List.of(sampleDto), 0, 10, 1L, 1, true, true
        );

        given(employeeService.getEmployees(any(), any(), any(), any(), any(), eq(0), eq(10), any(), any()))
                .willReturn(paged);

        mockMvc.perform(get("/api/employees")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].employeeCode").value("EMP-1001"))
                .andExpect(jsonPath("$.data.content[0].email").value("sarah.jenkins@workforcehub.io"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/employees?all=true - ADMIN can retrieve all unpaginated employees")
    @WithMockUser(roles = "ADMIN")
    void testAdminGetAllEmployeesSuccess() throws Exception {
        given(employeeService.getAllEmployees()).willReturn(List.of(sampleDto));

        mockMvc.perform(get("/api/employees?all=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].employeeCode").value("EMP-1001"));
    }

    @Test
    @DisplayName("GET /api/employees/{id} - ADMIN can retrieve employee by ID")
    @WithMockUser(roles = "ADMIN")
    void testAdminGetEmployeeByIdSuccess() throws Exception {
        given(employeeService.getEmployeeById(sampleId)).willReturn(sampleDto);

        mockMvc.perform(get("/api/employees/{id}", sampleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(sampleId.toString()))
                .andExpect(jsonPath("$.data.firstName").value("Sarah"));
    }

    @Test
    @DisplayName("GET /api/employees/{id} - Missing employee returns 404 Not Found")
    @WithMockUser(roles = "ADMIN")
    void testAdminGetNonexistentEmployeeReturns404() throws Exception {
        UUID nonexistent = UUID.randomUUID();
        given(employeeService.getEmployeeById(nonexistent))
                .willThrow(new ResourceNotFoundException("Employee with ID " + nonexistent + " not found"));

        mockMvc.perform(get("/api/employees/{id}", nonexistent))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("POST /api/employees - ADMIN without CSRF token returns 403 Forbidden")
    @WithMockUser(roles = "ADMIN")
    void testAdminPostWithoutCsrfTokenReturns403() throws Exception {
        EmployeeCreateRequest req = new EmployeeCreateRequest();
        req.setFirstName("Sarah");
        req.setLastName("Jenkins");
        req.setEmail("sarah.jenkins@workforcehub.io");
        req.setJobTitle("Engineer");
        req.setJoiningDate(LocalDate.now());

        // Performing mutating POST without .with(csrf())
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/employees - ADMIN with CSRF token creates employee (201 Created)")
    @WithMockUser(roles = "ADMIN")
    void testAdminPostWithCsrfCreatesEmployee() throws Exception {
        EmployeeCreateRequest req = new EmployeeCreateRequest();
        req.setFirstName("Sarah");
        req.setLastName("Jenkins");
        req.setEmail("sarah.jenkins@workforcehub.io");
        req.setJobTitle("Staff Software Engineer");
        req.setJoiningDate(LocalDate.of(2023, 3, 15));

        given(employeeService.createEmployee(any(EmployeeCreateRequest.class))).willReturn(sampleDto);

        mockMvc.perform(post("/api/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("sarah.jenkins@workforcehub.io"));
    }

    @Test
    @DisplayName("POST /api/employees - Duplicate email returns 409 Conflict")
    @WithMockUser(roles = "ADMIN")
    void testAdminPostDuplicateEmailReturns409() throws Exception {
        EmployeeCreateRequest req = new EmployeeCreateRequest();
        req.setFirstName("Sarah");
        req.setLastName("Jenkins");
        req.setEmail("sarah.jenkins@workforcehub.io");
        req.setJobTitle("Engineer");
        req.setJoiningDate(LocalDate.now());

        given(employeeService.createEmployee(any(EmployeeCreateRequest.class)))
                .willThrow(new DuplicateResourceException("Email address 'sarah.jenkins@workforcehub.io' is already in use"));

        mockMvc.perform(post("/api/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already in use")));
    }

    @Test
    @DisplayName("POST /api/employees - Invalid payload returns 400 Bad Request with field errors")
    @WithMockUser(roles = "ADMIN")
    void testAdminPostInvalidPayloadReturns400() throws Exception {
        EmployeeCreateRequest req = new EmployeeCreateRequest();
        req.setFirstName(""); // Blank first name
        req.setEmail("invalid-email-format"); // Invalid email

        mockMvc.perform(post("/api/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("PUT /api/employees/{id} - ADMIN with CSRF updates employee successfully")
    @WithMockUser(roles = "ADMIN")
    void testAdminPutEmployeeSuccess() throws Exception {
        EmployeeUpdateRequest req = new EmployeeUpdateRequest();
        req.setFirstName("Sarah");
        req.setLastName("Jenkins");
        req.setEmail("sarah.jenkins@workforcehub.io");
        req.setJobTitle("Senior Lead Engineer");
        req.setJoiningDate(LocalDate.of(2023, 3, 15));

        sampleDto.setJobTitle("Senior Lead Engineer");
        given(employeeService.updateEmployee(eq(sampleId), any(EmployeeUpdateRequest.class))).willReturn(sampleDto);

        mockMvc.perform(put("/api/employees/{id}", sampleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobTitle").value("Senior Lead Engineer"));
    }

    @Test
    @DisplayName("DELETE /api/employees/{id} - ADMIN with CSRF deletes employee successfully")
    @WithMockUser(roles = "ADMIN")
    void testAdminDeleteEmployeeSuccess() throws Exception {
        willDoNothing().given(employeeService).deleteEmployee(sampleId);

        mockMvc.perform(delete("/api/employees/{id}", sampleId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

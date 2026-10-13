package com.workforcehub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.workforcehub.config.CorsConfig;
import com.workforcehub.dto.DepartmentCreateRequest;
import com.workforcehub.dto.DepartmentDto;
import com.workforcehub.dto.DepartmentUpdateRequest;
import com.workforcehub.dto.PagedResponse;
import com.workforcehub.exception.BusinessRuleViolationException;
import com.workforcehub.exception.DuplicateResourceException;
import com.workforcehub.exception.GlobalExceptionHandler;
import com.workforcehub.exception.ResourceNotFoundException;
import com.workforcehub.security.SecurityConfig;
import com.workforcehub.service.DepartmentService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@Import({SecurityConfig.class, CorsConfig.class, GlobalExceptionHandler.class})
class DepartmentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentService departmentService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private DepartmentDto sampleDto;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleDto = new DepartmentDto();
        sampleDto.setId(sampleId);
        sampleDto.setDepartmentCode("DEPT-001");
        sampleDto.setName("Engineering");
        sampleDto.setDescription("Core software engineering team");
        sampleDto.setStatus("Active");
        sampleDto.setEmployeeCount(8L);
        sampleDto.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("GET /api/departments - Anonymous request returns 401 Unauthorized")
    void testAnonymousGetDepartmentsReturns401() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("POST /api/departments - Anonymous request returns 401 Unauthorized")
    void testAnonymousPostDepartmentsReturns401() throws Exception {
        DepartmentCreateRequest req = new DepartmentCreateRequest();
        req.setName("Quality Assurance");

        mockMvc.perform(post("/api/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/departments - EMPLOYEE role receives 403 Forbidden")
    @WithMockUser(roles = "EMPLOYEE")
    void testEmployeeRoleGetDepartmentsReturns403() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("POST /api/departments - EMPLOYEE role receives 403 Forbidden")
    @WithMockUser(roles = "EMPLOYEE")
    void testEmployeeRolePostDepartmentsReturns403() throws Exception {
        DepartmentCreateRequest req = new DepartmentCreateRequest();
        req.setName("Security Operations");

        mockMvc.perform(post("/api/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/departments?all=true - ADMIN can retrieve all departments")
    @WithMockUser(roles = "ADMIN")
    void testAdminGetAllDepartmentsSuccess() throws Exception {
        given(departmentService.getAllDepartments()).willReturn(List.of(sampleDto));

        mockMvc.perform(get("/api/departments?all=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].departmentCode").value("DEPT-001"))
                .andExpect(jsonPath("$.data[0].name").value("Engineering"))
                .andExpect(jsonPath("$.data[0].employeeCount").value(8));
    }

    @Test
    @DisplayName("GET /api/departments/{id} - ADMIN can retrieve department by ID")
    @WithMockUser(roles = "ADMIN")
    void testAdminGetDepartmentByIdSuccess() throws Exception {
        given(departmentService.getDepartmentById(sampleId)).willReturn(sampleDto);

        mockMvc.perform(get("/api/departments/{id}", sampleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(sampleId.toString()))
                .andExpect(jsonPath("$.data.name").value("Engineering"));
    }

    @Test
    @DisplayName("GET /api/departments/{id} - Missing department returns 404 Not Found")
    @WithMockUser(roles = "ADMIN")
    void testAdminGetNonexistentDepartmentReturns404() throws Exception {
        UUID nonexistent = UUID.randomUUID();
        given(departmentService.getDepartmentById(nonexistent))
                .willThrow(new ResourceNotFoundException("Department with ID " + nonexistent + " not found"));

        mockMvc.perform(get("/api/departments/{id}", nonexistent))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("POST /api/departments - ADMIN without CSRF token returns 403 Forbidden")
    @WithMockUser(roles = "ADMIN")
    void testAdminPostWithoutCsrfTokenReturns403() throws Exception {
        DepartmentCreateRequest req = new DepartmentCreateRequest();
        req.setName("Quality Assurance");

        // Mutating POST without csrf()
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/departments - ADMIN with CSRF token creates department (201 Created)")
    @WithMockUser(roles = "ADMIN")
    void testAdminPostWithCsrfCreatesDepartment() throws Exception {
        DepartmentCreateRequest req = new DepartmentCreateRequest();
        req.setName("Security");
        req.setDepartmentCode("DEPT-007");

        given(departmentService.createDepartment(any(DepartmentCreateRequest.class))).willReturn(sampleDto);

        mockMvc.perform(post("/api/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Engineering"));
    }

    @Test
    @DisplayName("POST /api/departments - Duplicate department name returns 409 Conflict")
    @WithMockUser(roles = "ADMIN")
    void testAdminPostDuplicateNameReturns409() throws Exception {
        DepartmentCreateRequest req = new DepartmentCreateRequest();
        req.setName("Engineering");

        given(departmentService.createDepartment(any(DepartmentCreateRequest.class)))
                .willThrow(new DuplicateResourceException("Department with name 'Engineering' already exists"));

        mockMvc.perform(post("/api/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/departments - Invalid blank name returns 400 Bad Request with validationErrors")
    @WithMockUser(roles = "ADMIN")
    void testAdminPostInvalidPayloadReturns400() throws Exception {
        DepartmentCreateRequest req = new DepartmentCreateRequest();
        req.setName(""); // Blank name violates @NotBlank

        mockMvc.perform(post("/api/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("PUT /api/departments/{id} - ADMIN with CSRF updates department successfully")
    @WithMockUser(roles = "ADMIN")
    void testAdminPutDepartmentSuccess() throws Exception {
        DepartmentUpdateRequest req = new DepartmentUpdateRequest();
        req.setName("Engineering & Data");

        sampleDto.setName("Engineering & Data");
        given(departmentService.updateDepartment(eq(sampleId), any(DepartmentUpdateRequest.class))).willReturn(sampleDto);

        mockMvc.perform(put("/api/departments/{id}", sampleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Engineering & Data"));
    }

    @Test
    @DisplayName("DELETE /api/departments/{id} - ADMIN with CSRF deletes empty department successfully")
    @WithMockUser(roles = "ADMIN")
    void testAdminDeleteDepartmentSuccess() throws Exception {
        willDoNothing().given(departmentService).deleteDepartment(sampleId);

        mockMvc.perform(delete("/api/departments/{id}", sampleId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/departments/{id} - Department with assigned employees returns 422 Unprocessable Entity")
    @WithMockUser(roles = "ADMIN")
    void testAdminDeleteDepartmentWithEmployeesReturns422() throws Exception {
        willThrow(new BusinessRuleViolationException("Cannot delete department with assigned employees. Reassign personnel first."))
                .given(departmentService).deleteDepartment(sampleId);

        mockMvc.perform(delete("/api/departments/{id}", sampleId)
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value(containsString("assigned employees")));
    }
}

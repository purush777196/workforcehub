package com.workforcehub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.workforcehub.config.CorsConfig;
import com.workforcehub.dto.importing.*;
import com.workforcehub.exception.GlobalExceptionHandler;
import com.workforcehub.security.SecurityConfig;
import com.workforcehub.service.ImportService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImportController.class)
@Import({SecurityConfig.class, CorsConfig.class, GlobalExceptionHandler.class})
class ImportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportService importService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Anonymous access to preview returns 401 Unauthorized")
    void testAnonymousAccessToPreviewReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/import/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Employee access to preview returns 403 Forbidden")
    @WithMockUser(username = "employee@workforcehub.io", roles = {"EMPLOYEE"})
    void testEmployeeAccessToPreviewReturns403() throws Exception {
        mockMvc.perform(post("/api/admin/import/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin access to preview without CSRF token returns 403 Forbidden")
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    void testAdminAccessToPreviewWithoutCsrfReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/import/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin access to preview with valid CSRF succeeds with 200 OK")
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    void testAdminAccessToPreviewWithCsrfSucceeds() throws Exception {
        ImportPreviewResponseDto previewDto = new ImportPreviewResponseDto();
        previewDto.setPreviewId(UUID.randomUUID());
        previewDto.setReadyForExecution(true);

        given(importService.previewImport(any(), any())).willReturn(previewDto);

        mockMvc.perform(post("/api/admin/import/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.readyForExecution").value(true));
    }

    @Test
    @DisplayName("Anonymous access to execute returns 401 Unauthorized")
    void testAnonymousAccessToExecuteReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/import/execute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Employee access to execute returns 403 Forbidden")
    @WithMockUser(username = "employee@workforcehub.io", roles = {"EMPLOYEE"})
    void testEmployeeAccessToExecuteReturns403() throws Exception {
        mockMvc.perform(post("/api/admin/import/execute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmBackupTaken\": true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin access to execute without CSRF returns 403 Forbidden")
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    void testAdminAccessToExecuteWithoutCsrfReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/import/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmBackupTaken\": true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin access to execute with valid CSRF succeeds with 200 OK")
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    void testAdminAccessToExecuteWithCsrfSucceeds() throws Exception {
        ImportResultDto resultDto = new ImportResultDto();
        resultDto.setJobId(UUID.randomUUID());
        resultDto.setStatus("SUCCESS");

        given(importService.executeImport(any(), any())).willReturn(resultDto);

        ImportExecuteRequestDto req = new ImportExecuteRequestDto();
        req.setConfirmBackupTaken(true);
        req.setConflictStrategy("SKIP_EXISTING");

        mockMvc.perform(post("/api/admin/import/execute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("Admin can retrieve job status by ID")
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    void testAdminGetJobStatusSucceeds() throws Exception {
        UUID jobId = UUID.randomUUID();
        ImportJobDto jobDto = new ImportJobDto();
        jobDto.setId(jobId);
        jobDto.setStatus("SUCCESS");

        given(importService.getJobStatus(jobId)).willReturn(jobDto);

        mockMvc.perform(get("/api/admin/import/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(jobId.toString()));
    }

    @Test
    @DisplayName("Employee cannot retrieve job status by ID (403 Forbidden)")
    @WithMockUser(username = "employee@workforcehub.io", roles = {"EMPLOYEE"})
    void testEmployeeGetJobStatusReturns403() throws Exception {
        UUID jobId = UUID.randomUUID();
        mockMvc.perform(get("/api/admin/import/jobs/" + jobId))
                .andExpect(status().isForbidden());
    }
}

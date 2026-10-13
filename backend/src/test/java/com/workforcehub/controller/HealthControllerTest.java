package com.workforcehub.controller;

import com.workforcehub.dto.DatabaseHealthResponse;
import com.workforcehub.service.HealthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.workforcehub.config.CorsConfig;
import com.workforcehub.security.SecurityConfig;
import org.springframework.context.annotation.Import;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class HealthControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @Test
    @DisplayName("GET /api/health returns 200 OK with structured API envelope")
    void testGetApplicationHealth() throws Exception {
        given(healthService.getApplicationHealth()).willReturn(Map.of(
            "status", "UP",
            "application", "WorkforceHub Backend",
            "version", "0.0.1-SNAPSHOT"
        ));

        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("UP"))
            .andExpect(jsonPath("$.data.application").value("WorkforceHub Backend"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /api/health/database returns 200 OK when PostgreSQL connection is established")
    void testGetDatabaseHealthWhenConnected() throws Exception {
        given(healthService.checkDatabaseHealth()).willReturn(
            DatabaseHealthResponse.up(15)
        );

        mockMvc.perform(get("/api/health/database"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("UP"))
            .andExpect(jsonPath("$.data.connected").value(true))
            .andExpect(jsonPath("$.data.database").value("PostgreSQL"))
            .andExpect(jsonPath("$.data.responseTimeMs").value(15));
    }

    @Test
    @DisplayName("GET /api/health/database returns 503 SERVICE_UNAVAILABLE when connection fails")
    void testGetDatabaseHealthWhenDisconnected() throws Exception {
        given(healthService.checkDatabaseHealth()).willReturn(
            DatabaseHealthResponse.down(200, "Database connection unavailable.")
        );

        mockMvc.perform(get("/api/health/database"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.data.status").value("DOWN"))
            .andExpect(jsonPath("$.data.connected").value(false))
            .andExpect(jsonPath("$.data.message").value("Database connection unavailable."));
    }
}

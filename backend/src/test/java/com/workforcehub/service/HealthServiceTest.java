package com.workforcehub.service;

import com.workforcehub.dto.DatabaseHealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    private HealthService healthService;

    @BeforeEach
    void setUp() {
        healthService = new HealthService(dataSource);
    }

    @Test
    @DisplayName("Application health check returns status UP and version information")
    void testGetApplicationHealth() {
        Map<String, Object> health = healthService.getApplicationHealth();
        assertThat(health).isNotNull();
        assertThat(health.get("status")).isEqualTo("UP");
        assertThat(health.get("application")).isEqualTo("WorkforceHub Backend");
        assertThat(health.get("version")).isEqualTo("0.0.1-SNAPSHOT");
        assertThat(health.get("startedAt")).isNotNull();
    }

    @Test
    @DisplayName("Database probe returns status UP when connection is valid")
    void testDatabaseProbeSuccess() throws Exception {
        given(dataSource.getConnection()).willReturn(connection);
        given(connection.isValid(anyInt())).willReturn(true);

        DatabaseHealthResponse response = healthService.checkDatabaseHealth();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("UP");
        assertThat(response.isConnected()).isTrue();
        assertThat(response.getDatabase()).isEqualTo("PostgreSQL");
        assertThat(response.getMessage()).contains("healthy");
    }

    @Test
    @DisplayName("Database probe returns status DOWN with sanitized error when connection throws SQLException")
    void testDatabaseProbeFailure() throws Exception {
        given(dataSource.getConnection()).willThrow(new SQLException("Connection refused to internal:5432 with user 'secret_user'"));

        DatabaseHealthResponse response = healthService.checkDatabaseHealth();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("DOWN");
        assertThat(response.isConnected()).isFalse();
        // Verifies that credentials and internal hostnames are NOT leaked to caller
        assertThat(response.getMessage()).doesNotContain("secret_user");
        assertThat(response.getMessage()).doesNotContain("5432");
        assertThat(response.getMessage()).contains("Database connection unavailable");
    }
}

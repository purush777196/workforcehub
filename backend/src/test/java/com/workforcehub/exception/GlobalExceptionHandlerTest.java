package com.workforcehub.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

    @Test
    @DisplayName("ResourceNotFoundException maps to 404 Not Found")
    void testHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Employee with ID 123 not found");
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Employee with ID 123 not found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("DuplicateResourceException maps to 409 Conflict")
    void testHandleDuplicateResource() {
        DuplicateResourceException ex = new DuplicateResourceException("Department code 'ENG' already exists");
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResource(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).contains("already exists");
    }

    @Test
    @DisplayName("BusinessRuleViolationException maps to 422 Unprocessable Entity")
    void testHandleBusinessRuleViolation() {
        BusinessRuleViolationException ex = new BusinessRuleViolationException("End date cannot precede start date");
        ResponseEntity<ErrorResponse> response = handler.handleBusinessRuleViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(422);
        assertThat(response.getBody().getMessage()).contains("End date cannot precede start date");
    }

    @Test
    @DisplayName("DataIntegrityViolationException sanitizes internal database messages")
    void testHandleDataIntegrityViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
            "ERROR: duplicate key value violates unique constraint 'uq_employees_email' Key (email)=(elena@workforcehub.com)"
        );
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        // Verifies raw SQL and table constraint details are hidden
        assertThat(response.getBody().getMessage()).doesNotContain("uq_employees_email");
        assertThat(response.getBody().getMessage()).contains("violates database integrity");
    }
}

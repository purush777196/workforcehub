package com.workforcehub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workforcehub.config.CorsConfig;
import com.workforcehub.dto.LoginRequest;
import com.workforcehub.entity.UserAccount;
import com.workforcehub.entity.enums.Role;
import com.workforcehub.security.CustomUserDetails;
import com.workforcehub.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /api/auth/login with valid credentials authenticates successfully")
    void testLoginSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UserAccount account = new UserAccount("admin@workforcehub.io", "$2a$12$hashedPassword", Role.ADMIN);
        account.setId(userId);

        CustomUserDetails userDetails = new CustomUserDetails(
            account, "Alex Morgan", null, "People & HR", "HR Administrator"
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities()
        );

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(auth);

        LoginRequest request = new LoginRequest("admin@workforcehub.io", "AdminSecure2026!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("admin@workforcehub.io"))
            .andExpect(jsonPath("$.data.name").value("Alex Morgan"))
            .andExpect(jsonPath("$.data.role").value("ADMIN"))
            // Verify password hashes and session internal tokens are strictly absent
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/login with incorrect password returns generic 401 Unauthorized")
    void testLoginBadCredentials() throws Exception {
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .willThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest("admin@workforcehub.io", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Invalid email or password. Please verify your credentials."));
    }

    @Test
    @DisplayName("POST /api/auth/login for disabled account returns 401 Unauthorized")
    void testLoginDisabledAccount() throws Exception {
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .willThrow(new DisabledException("Account is disabled"));

        LoginRequest request = new LoginRequest("disabled@workforcehub.io", "AnyPassword!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value(containsString("Account is disabled")));
    }


    @Test
    @DisplayName("GET /api/auth/me rejects anonymous request with 401 Unauthorized")
    void testGetMeAnonymous() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/auth/csrf returns valid CSRF token payload")
    void testGetCsrfToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").exists())
            .andExpect(jsonPath("$.data.headerName").value("X-CSRF-TOKEN"));
    }

    @Test
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    @DisplayName("POST /api/auth/logout without CSRF token is rejected with 403 Forbidden")
    void testLogoutWithoutCsrfRejected() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@workforcehub.io", roles = {"ADMIN"})
    @DisplayName("POST /api/auth/logout with valid CSRF token succeeds and clears session")
    void testLogoutWithCsrfSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(header().exists("Set-Cookie"));
    }
}

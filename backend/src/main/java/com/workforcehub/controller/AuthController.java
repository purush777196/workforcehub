package com.workforcehub.controller;

import com.workforcehub.dto.ApiResponse;
import com.workforcehub.dto.AuthResponse;
import com.workforcehub.dto.LoginRequest;
import com.workforcehub.entity.enums.Role;
import com.workforcehub.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication REST Controller
 * 
 * Manages database-backed login, logout, session restoration, and CSRF token distribution.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean sessionCookieSecure;

    @Autowired
    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Authenticates user credentials against PostgreSQL and establishes a server-managed session.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String normalizedEmail = loginRequest.getEmail().trim().toLowerCase();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, loginRequest.getPassword())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            AuthResponse authResponse = buildAuthResponse(userDetails);

            log.info("User [{}] authenticated successfully with role [{}].", normalizedEmail, authResponse.getRole());
            return ResponseEntity.ok(ApiResponse.ok("Login successful", authResponse));

        } catch (DisabledException ex) {
            log.warn("Login attempt for disabled account [{}].", normalizedEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Account is disabled. Please contact your administrator."));
        } catch (BadCredentialsException ex) {
            log.warn("Invalid credentials submitted for [{}].", normalizedEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password. Please verify your credentials."));
        } catch (Exception ex) {
            log.error("Authentication failed unexpectedly for [{}]:", normalizedEmail, ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password. Please verify your credentials."));
        }
    }

    /**
     * Retrieves currently authenticated user's profile from the session.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No active session found. Please sign in."));
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid session principal."));
        }

        AuthResponse authResponse = buildAuthResponse(userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Active session verified", authResponse));
    }

    /**
     * Invalidates the server-side session and clears the session cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Issue deletion cookie
        ResponseCookie deleteCookie = ResponseCookie.from("JSESSIONID", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(sessionCookieSecure)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    /**
     * Supplies CSRF token metadata for client-side state-changing requests.
     */
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCsrfToken(CsrfToken csrfToken) {
        if (csrfToken == null) {
            return ResponseEntity.ok(ApiResponse.ok("CSRF token not required", Map.of()));
        }
        return ResponseEntity.ok(ApiResponse.ok("CSRF token generated", Map.of(
                "token", csrfToken.getToken(),
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName()
        )));
    }

    private AuthResponse buildAuthResponse(CustomUserDetails userDetails) {
        String roleStr = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("EMPLOYEE");

        Role role = Role.valueOf(roleStr);

        String initials = "WH";
        if (userDetails.getFullName() != null && !userDetails.getFullName().isBlank()) {
            String[] parts = userDetails.getFullName().trim().split("\\s+");
            if (parts.length >= 2) {
                initials = (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
            } else if (parts.length == 1 && parts[0].length() >= 2) {
                initials = parts[0].substring(0, 2).toUpperCase();
            }
        }

        return new AuthResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getFullName(),
                role,
                userDetails.getDepartmentName(),
                userDetails.getEmployeeCode(),
                initials,
                userDetails.getJobTitle()
        );
    }
}

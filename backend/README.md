# WorkforceHub Backend — Phase 8B: Production-Quality Authentication & Server-Side Authorization

This directory houses the **Spring Boot 3.4.3 REST API backend** for WorkforceHub. Phase 8B establishes real database-backed authentication, server-side authorization, cookie-based session management, SPA-compatible CSRF protection, and initial administrator bootstrap provisioning.

---

## 1. Prerequisites and Supported Versions

| Dependency | Required Version | Host Verified Version |
| :--- | :--- | :--- |
| **Java Development Kit (JDK)** | Java 21+ LTS (Java 21 bytecode target) | Oracle JDK 24.0.2 / OpenJDK 21+ |
| **Build Tool (Maven)** | Maven 3.9+ | Apache Maven 3.9.16 (Wrapper included via `mvnw.cmd` / `mvnw`) |
| **Relational Database** | PostgreSQL 15+ | PostgreSQL 18.4 on `localhost:5432` |
| **Framework** | Spring Boot 3.4.3 / Spring Security 6.4.3 | Spring Boot 3.4.3 |

---

## 2. Environment Variables Configuration

Copy `.env.example` to `.env` or export variables in your execution environment. **Never commit passwords or secrets to source control.**

```bash
# Server Port
SERVER_PORT=8080

# PostgreSQL Database Credentials
DB_HOST=localhost
DB_PORT=5432
DB_NAME=workforcehub_db
DB_USERNAME=postgres
DB_PASSWORD=your_local_postgres_password

# Allowed Frontend Origins (CORS) - Credentials enabled (never use *)
CORS_ALLOWED_ORIGINS=http://localhost:5173

# Session Cookie Configuration (false for local HTTP development, true for production HTTPS)
SESSION_COOKIE_SECURE=false

# Initial Account Provisioning Bootstrap
ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_BOOTSTRAP_EMAIL=admin@workforcehub.io
ADMIN_BOOTSTRAP_PASSWORD=Admin@123456
ADMIN_BOOTSTRAP_FULLNAME=Alex Morgan

EMPLOYEE_BOOTSTRAP_EMAIL=employee@workforcehub.io
EMPLOYEE_BOOTSTRAP_PASSWORD=Employee@123456
EMPLOYEE_BOOTSTRAP_FULLNAME=Elena Rostova

# JPA & Migration Behavior
JPA_SHOW_SQL=false
FLYWAY_ENABLED=true
```

---

## 3. Database Migrations with Flyway

Flyway migrations are located in `src/main/resources/db/migration/`:
- `V1__initial_schema.sql`: Establishes `user_accounts`, `departments`, `employees`, `attendance_records`, and `leave_requests` tables, indexes, and referential constraints.

Hibernate DDL is configured to `ddl-auto: validate`, ensuring Hibernate never mutates the database schema directly and strictly validates against Flyway's managed state.

---

## 4. Initial Account Provisioning (Bootstrap)

To avoid hardcoded credentials or insecure self-registration, WorkforceHub includes `AdminBootstrapService`:
- Runs automatically on application startup via `ApplicationRunner`.
- Checks whether `admin@workforcehub.io` and `employee@workforcehub.io` exist in `user_accounts`.
- If absent, provisions the account with a BCrypt hash (cost factor 12) and creates corresponding linked `Employee` records.
- **Refuses to overwrite** existing accounts or reset altered passwords.
- Can be completely disabled in production by setting `ADMIN_BOOTSTRAP_ENABLED=false`.

Default local development credentials provisioned:
- **Administrator**: `admin@workforcehub.io` / `Admin@123456` (Role: `ADMIN`)
- **Employee**: `employee@workforcehub.io` / `Employee@123456` (Role: `EMPLOYEE`)

---

## 5. Authentication Architecture & Security Configuration

### Session Management
- Server-managed session stored in an `HttpOnly`, `SameSite=Lax` cookie named `JSESSIONID`.
- `Secure=true` in production HTTPS deployments (configured via `SESSION_COOKIE_SECURE`).
- Session fixation protection enabled using `migrateSession()`.
- Explicit logout endpoint invalidates server session and sends a clearing cookie (`maxAge=0`).

### CSRF Protection
- Cookie-based CSRF protection utilizing `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
- SPA integration handled via `SpaCsrfTokenRequestHandler` and `CsrfCookieFilter`.
- State-changing requests (`POST`, `PUT`, `DELETE`, `PATCH`) require `X-XSRF-TOKEN` or `X-CSRF-TOKEN` header.
- Endpoint `GET /api/auth/csrf` provides CSRF token metadata to client SPAs.

### CORS Settings
- Explicit origin binding to `http://localhost:5173`.
- `allowCredentials = true` configured with exact origin matching (**never** wildcard `*`).

### Generic Error Responses
- Authentication failures return generic messages (`401 Unauthorized`) to prevent account enumeration attacks.
- Passwords, password hashes, and internal authentication objects are strictly excluded from API responses.

---

## 6. Authentication Endpoints

### 1. `POST /api/auth/login`
Authenticates credentials against PostgreSQL and issues `JSESSIONID` cookie.

**Request Body:**
```json
{
  "email": "admin@workforcehub.io",
  "password": "Admin@123456"
}
```

**Response (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "admin@workforcehub.io",
    "name": "Alex Morgan",
    "role": "ADMIN",
    "department": "Executive Management",
    "employeeCode": null,
    "avatarInitials": "AM",
    "jobTitle": "System Administrator"
  },
  "timestamp": "2026-10-13T14:30:00Z"
}
```

### 2. `GET /api/auth/me`
Retrieves currently authenticated session principal and verified role. Rejects anonymous access with HTTP 401.

**Response (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Active session verified",
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "admin@workforcehub.io",
    "name": "Alex Morgan",
    "role": "ADMIN",
    "department": "Executive Management",
    "employeeCode": null,
    "avatarInitials": "AM",
    "jobTitle": "System Administrator"
  },
  "timestamp": "2026-10-13T14:30:00Z"
}
```

### 3. `POST /api/auth/logout`
Requires authenticated session and valid CSRF token header (`X-XSRF-TOKEN`). Invalidates server session and clears cookie.

**Response (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null,
  "timestamp": "2026-10-13T14:30:00Z"
}
```

### 4. `GET /api/auth/csrf`
Supplies CSRF token metadata for client-side state-changing requests.

**Response (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "CSRF token generated",
  "data": {
    "token": "496bebf8-2b87-43cf-bc08-b0d7718e00ee",
    "headerName": "X-CSRF-TOKEN",
    "parameterName": "_csrf"
  },
  "timestamp": "2026-10-13T14:30:00Z"
}
```

---

## 7. Starting the Backend Locally

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="workforcehub_db"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_postgres_password"

.\mvnw.cmd spring-boot:run
```

The server starts on `http://localhost:8080`.

---

## 8. Running the Security Test Suite

Run the full automated test suite using the Maven wrapper:
```powershell
.\mvnw.cmd test
```

### Test Suite Summary (28 Tests, 0 Failures):
1. **`AuthControllerSecurityTest` (7 tests)**:
   - Valid credentials return HTTP 200 and safe profile without password hashes or internal IDs.
   - Invalid credentials return generic HTTP 401.
   - Disabled accounts return HTTP 401.
   - Anonymous access to `/api/auth/me` returns HTTP 401 with standard JSON error envelope.
   - CSRF token endpoint `/api/auth/csrf` returns valid token payload.
   - `POST /api/auth/logout` without CSRF token is rejected with HTTP 403 Forbidden.
   - `POST /api/auth/logout` with valid CSRF succeeds and invalidates cookie.
2. **`CustomUserDetailsServiceTest` (3 tests)**:
   - ADMIN authorities mapped correctly (`ROLE_ADMIN`).
   - EMPLOYEE authorities mapped correctly (`ROLE_EMPLOYEE`).
   - Disabled accounts throw `DisabledException`.
3. **`HealthControllerTest` (3 tests)**:
   - Application liveness and PostgreSQL connection probes.
4. **`HealthServiceTest` (3 tests)**:
   - Sanitized database health check handling.
5. **`GlobalExceptionHandlerTest` (4 tests)**:
   - 404, 409, 422, and 500 error sanitization.
6. **`ValidationTest` (4 tests)**:
   - Jakarta Bean Validation on DTOs.
7. **`EntityMappingTest` (4 tests)**:
   - JPA entity projection and bidirectional relationship integrity.

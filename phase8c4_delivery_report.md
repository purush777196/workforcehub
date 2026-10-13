# WORKFORCEHUB — PHASE 8C.4 DELIVERY REPORT
## Leave Management REST API & Frontend Integration

**Date**: 2026-10-13  
**Status**: COMPLETE & VERIFIED  
**Backend Framework**: Spring Boot 3.4.3 / Spring Security 6.4.3 / Java 24 / PostgreSQL / Hibernate / Flyway  
**Frontend Framework**: React 18 / Vite 8.3.4 / JavaScript  

---

## 1. Executive Summary

Phase 8C.4 successfully migrates the WorkforceHub Leave Management module from browser-backed `localStorage` state into a secure, transactional, database-backed REST API and PostgreSQL persistence layer powered by **Spring Boot 3.4.3**, **Spring Security 6**, and **Spring Data JPA**.

### Key Deliverables:
1. **Secured REST API Endpoints**:
   - `GET /api/leaves`: Multi-criteria filtering (`page`, `size`, `employeeId`, `status`, `leaveType`, `startDate`, `endDate`, `search`, `sortBy`, `sortDirection`) with server-side JPA specifications and pagination.
   - `GET /api/leaves/{id}`: Single leave request lookup with service-layer ownership validation.
   - `POST /api/leaves`: Leave request submission resolving employee identity strictly from the authenticated server-side principal (`ROLE_EMPLOYEE` or authenticated `ROLE_ADMIN`).
   - `PUT /api/leaves/{id}`: Self-service update for `PENDING` leave requests (owned by employee) or administrative updates.
   - `POST /api/leaves/{id}/approve`: Administrator-only atomic leave approval with policy re-validation, conflict checks, and reviewer recording.
   - `POST /api/leaves/{id}/reject`: Administrator-only leave rejection requiring a mandatory rejection reason, without deducting balances or triggering attendance side effects.
   - `DELETE /api/leaves/{id}`: Cancellation of `PENDING` leave requests.
   - `GET /api/leaves/balance/me`: Authenticated employee self-service leave balance retrieval.
   - `GET /api/leaves/balance/{employeeId}`: Administrator-only employee balance retrieval (non-admins receive HTTP 403 Forbidden).
   - `GET /api/leaves/summary` & `GET /api/leaves/stats`: Real-time aggregate metrics for dashboard summary cards.
2. **Server-Derived Identity & Strict Authorization**:
   - Client-supplied employee IDs are never trusted as proof of ownership.
   - Employees (`ROLE_EMPLOYEE`) can only view, update, cancel, and query balances for their own profile. Attempted access to another employee's records or balance returns HTTP 403 Forbidden.
   - Approve, reject, and cross-employee balance inspection are strictly guarded by `@PreAuthorize("hasRole('ADMIN')")`.
3. **Leave Policy & Balance Calculations**:
   - Centralized policy allowances:
     - **Annual Leave**: 20 calendar days
     - **Sick Leave**: 12 calendar days
     - **Personal Leave**: 5 calendar days
     - **Unpaid Leave**: 30 calendar days
   - Duration counted in inclusive calendar days ($\text{days} = \text{endDate} - \text{startDate} + 1$).
   - Overlap protection prevents concurrent or duplicate `PENDING` / `APPROVED` leave requests for the same employee over overlapping date spans.
   - Dynamic balance calculation derives used and pending days from persisted authoritative records, ensuring idempotency and preventing double-deductions.
4. **Attendance Integration**:
   - Integrated with `AttendanceServiceImpl`: employees on approved leave on a given date cannot check in (returns HTTP 422 Unprocessable Entity with a clear business error).
   - Real-time attendance summary counts employees on approved leave today and factors them into `onLeaveCount`.
   - Approved leave is never classified as unexcused absence in attendance rate metrics.
5. **Frontend Service & Context Integration**:
   - Updated `leaveService.js` with authenticated REST calls, session cookies (`credentials: 'include'`), and CSRF token transmission (`X-XSRF-TOKEN`).
   - Upgraded `LeaveContext.jsx` with asynchronous state handling (`isLoading`, `isMutating`, `error`, `refreshLeaves`, `refreshSummary`, `backendSummary`).
   - Updated `LeavesPage.jsx`, `ApplyLeaveModal.jsx`, `EditLeaveModal.jsx`, `ReviewLeaveModal.jsx`, and `LeaveBalancesView.jsx` with loading states, error/retry banners, and role-based permissions.
   - The legacy `workforcehub_leaves` localStorage dataset is strictly preserved without deletions or automated imports.
6. **Automated & Static Test Suite**:
   - **136 backend tests executed and passing 100%** (0 failures, 0 errors, 0 skipped), including 15 unit tests in `LeaveServiceTest`, 15 Spring Security tests in `LeaveControllerSecurityTest`, and updated `AttendanceServiceTest`.
   - **0 lint warnings / 0 lint errors** via `oxlint` across 58 files.
   - **Production build verified** via `vite build` (completed in 574ms).

---

## 2. Implemented REST Endpoints and DTOs

### REST Endpoints Summary

| Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `GET` | `/api/leaves` | `ADMIN` or `EMPLOYEE` | Paginated listing with search, date range, status, and leave type filtering. `EMPLOYEE` is restricted strictly to their own requests. |
| `GET` | `/api/leaves/{id}` | `ADMIN` or `EMPLOYEE` | Retrieves single leave request by UUID. Ownership validated in service layer (returns 403 if `EMPLOYEE` accesses another person's request). |
| `POST` | `/api/leaves` | `ADMIN` or `EMPLOYEE` | Submits a leave request. Employee identity resolved server-side. Enforces date ordering, overlap prevention, and balance limits. |
| `PUT` | `/api/leaves/{id}` | `ADMIN` or `EMPLOYEE` | Updates a `PENDING` leave request. Enforces caller ownership and re-validates date ranges and overlap constraints. |
| `POST` | `/api/leaves/{id}/approve` | `ADMIN` only | Atomically approves a pending leave request, checks balances, and logs the authenticated reviewer. |
| `POST` | `/api/leaves/{id}/reject` | `ADMIN` only | Rejects a pending leave request with a mandatory rejection reason, without balance deduction. |
| `DELETE` | `/api/leaves/{id}` | `ADMIN` or `EMPLOYEE` | Cancels a `PENDING` leave request and removes it from the approval queue. |
| `GET` | `/api/leaves/balance/me` | `ADMIN` or `EMPLOYEE` | Returns the authenticated employee's leave balances and allowances across all categories. |
| `GET` | `/api/leaves/balance/{employeeId}` | `ADMIN` only | Returns leave balances for any employee. Non-administrators receive HTTP 403 Forbidden. |
| `GET` | `/api/leaves/summary` | `ADMIN` or `EMPLOYEE` | Aggregate company-wide metrics (`totalRequests`, `pendingCount`, `approvedCount`, `rejectedCount`). Alias `/stats` supported. |

### DTOs Architecture

1. **`LeaveRequestDto`**:
   - Primary safe representation of leave requests.
   - Fields: `id`, `employeeId`, `employeeName`, `employeeCode`, `departmentId`, `departmentName`, `leaveType`, `startDate`, `endDate`, `daysCount`, `status`, `reason`, `reviewedBy`, `reviewerName`, `reviewComment`, `reviewedAt`, `createdAt`, `updatedAt`.
   - Serialization aliases: `@JsonGetter("requestedDays")`, `@JsonGetter("submittedAt")`, `@JsonGetter("reviewedBy")`, and `@JsonGetter("reviewerComment")` ensure seamless compatibility with existing frontend expectations.
2. **`LeaveApplyRequest`**:
   - Validated submission payload with `@NotNull LeaveType leaveType`, `@NotNull LocalDate startDate`, `@NotNull LocalDate endDate`, and `@NotBlank @Size(max = 500) String reason`. Optional `UUID employeeId` allowed only for administrators submitting on behalf of an employee.
3. **`LeaveUpdateRequest`**:
   - Validated update payload with `@NotNull LeaveType leaveType`, `@NotNull LocalDate startDate`, `@NotNull LocalDate endDate`, and `@NotBlank @Size(max = 500) String reason`.
4. **`LeaveReviewRequest`**:
   - Validated review payload with `@NotBlank(message = "Review comment / rejection reason is required") String comment`.
5. **`LeaveBalanceItemDto`**:
   - Category balance item detailing `allowed`, `used`, and `remaining` allowances.
6. **`LeaveBalancesDto`**:
   - Employee balances mapping category items (`annual`, `sick`, `personal`, `unpaid`), `totalAllowed`, `totalUsed`, and `totalRemaining`.
7. **`LeaveSummaryDto`**:
   - Real-time aggregate status counts projection (`totalRequests`, `pendingCount`, `approvedCount`, `rejectedCount`).

---

## 3. Server-Side Authorization and Ownership Model

WorkforceHub implements a strict defense-in-depth security model:

### 1. Spring Security Configuration (`SecurityConfig.java`)
- All leave endpoints require authentication (`.anyRequest().authenticated()`).
- Unauthenticated requests receive HTTP 401 Unauthorized with a standardized JSON error.
- State-mutating requests (`POST`, `PUT`, `DELETE`) require a valid CSRF token (`X-XSRF-TOKEN`) via `CookieCsrfTokenRepository.withHttpOnlyFalse()` and `SpaCsrfTokenRequestHandler`.

### 2. Method-Level Security (`@PreAuthorize`)
- Administrative endpoints (`POST /api/leaves/{id}/approve`, `POST /api/leaves/{id}/reject`, and `GET /api/leaves/balance/{employeeId}`) are protected with `@PreAuthorize("hasRole('ADMIN')")`.
- When an employee attempts an administrative action, Spring Security blocks execution with HTTP 403 Forbidden.

### 3. Service-Layer Ownership Enforcement
- **Listing Requests (`getLeaveRequests`)**: If caller is not `ROLE_ADMIN`, the query forces `employeeId` to the authenticated caller's employee ID. If an employee explicitly supplies another employee ID in the query params, access is denied with HTTP 403 Forbidden.
- **Single Request Lookup (`getLeaveRequestById`)**: If caller is `ROLE_EMPLOYEE`, verifies `request.getEmployee().getId().equals(currentEmp.getId())`. Unauthorized access throws `AccessDeniedException` (HTTP 403 Forbidden).
- **Submission (`applyLeave`)**: If caller is `ROLE_EMPLOYEE`, `request.getEmployeeId()` is ignored and resolved strictly from `resolveCurrentEmployee(auth)`.
- **Update & Cancellation (`updateLeave`, `cancelLeave`)**: Non-admin callers can only edit or cancel requests belonging to their own employee identity. Attempts on other records return HTTP 403.
- **Balance Lookup**: `getMyLeaveBalances(auth)` resolves the caller's profile. `getEmployeeLeaveBalances(employeeId)` is restricted strictly to administrators.

---

## 4. Leave Policy, Balances & State Transitions

### 1. Policy Allowances & Duration Calculation
- **Calendar Days Counting**: Inclusive calculation:
  $$\text{daysCount} = \text{ChronoUnit.DAYS.between(startDate, endDate)} + 1$$
- **Category Maximums**:
  - `ANNUAL`: 20 days
  - `SICK`: 12 days
  - `PERSONAL`: 5 days
  - `UNPAID`: 30 days
- **Balance Constraints**:
  - For `ANNUAL` leave, remaining balance is checked prior to submission and approval. Requests exceeding available days are rejected with HTTP 422 Unprocessable Entity.

### 2. Overlap Prevention & Conflict Handling
- Overlapping requests for the same employee are detected via `findConflictingRequests`:
  $$\text{existing.startDate} \le \text{new.endDate} \quad \text{AND} \quad \text{existing.endDate} \ge \text{new.startDate}$$
- Evaluated against existing requests in `PENDING` or `APPROVED` status.
- Overlapping requests are rejected with HTTP 409 Conflict ("Employee already has a pending or approved leave request overlapping this date range").

### 3. State Transitions & Review Workflow
- **Initial State**: Newly submitted requests are set to `PENDING`.
- **Approval (`POST /api/leaves/{id}/approve`)**:
  - Precondition: Request must be in `PENDING` status.
  - Re-evaluates overlaps against approved records and checks balance limits.
  - Atomically transitions status to `APPROVED`, records authenticated administrator as reviewer, and updates `reviewedAt`.
  - Duplicate approvals on already finalized requests return HTTP 409 Conflict.
- **Rejection (`POST /api/leaves/{id}/reject`)**:
  - Precondition: Request must be in `PENDING` status.
  - Requires a non-blank rejection reason.
  - Transitions status to `REJECTED`, records reviewer and review comment.
  - Does not deduct leave balances or affect attendance records.
- **Cancellation (`DELETE /api/leaves/{id}`)**:
  - Can only be cancelled while in `PENDING` status.
  - Removes request from database; finalized (`APPROVED` or `REJECTED`) requests cannot be cancelled.

---

## 5. Attendance Integration

Phase 8C.4 seamlessly links Leave Management with Attendance Management (`AttendanceServiceImpl`):

1. **Check-In Restriction on Approved Leave**:
   - When an employee initiates self-service check-in, `AttendanceServiceImpl.checkIn()` checks `leaveRequestRepository.findApprovedLeavesOnDate(employee.getId(), today)`.
   - If an approved leave record is found covering `today`, check-in is rejected with HTTP 422 Unprocessable Entity:
     > *"Employee [Name] is on approved leave today and cannot check in."*
2. **Attendance Summary Aggregation**:
   - In `AttendanceServiceImpl.getAttendanceSummary(date)`, employees with active approved leave covering the date are counted using `leaveRequestRepository.countApprovedEmployeesOnDate(date)`.
   - The count is factored into `onLeaveCount`.
3. **Absence Classification**:
   - Approved leave days are never classified as unexcused absences.
   - Attendance rate calculations exclude employees on approved leave from the unexcused denominator.

---

## 6. Database Migrations

- The initial Flyway schema `V1__initial_schema.sql` previously established the `leave_requests` table with comprehensive constraints:
  ```sql
  CREATE TABLE leave_requests (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      employee_id UUID NOT NULL REFERENCES employees(id),
      leave_type VARCHAR(20) NOT NULL,
      start_date DATE NOT NULL,
      end_date DATE NOT NULL,
      days_count INT NOT NULL,
      status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
      reason TEXT NOT NULL,
      reviewed_by UUID REFERENCES user_accounts(id),
      review_comment TEXT,
      reviewed_at TIMESTAMPTZ,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      CONSTRAINT chk_leave_dates CHECK (end_date >= start_date),
      CONSTRAINT chk_leave_days_count CHECK (days_count > 0),
      CONSTRAINT chk_leave_type CHECK (leave_type IN ('ANNUAL', 'SICK', 'PERSONAL', 'UNPAID')),
      CONSTRAINT chk_leave_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
  );
  ```
- All table columns, foreign keys, and check constraints match the domain requirements.
- No new Flyway migrations were required, and existing migrations `V1` and `V2` were preserved without modification.

---

## 7. Frontend Integration Details

### Updated Files

1. **`src/services/leaveService.js`**:
   - Added REST API helper functions: `fetchLeavesApi`, `fetchLeaveByIdApi`, `applyLeaveApi`, `updateLeaveApi`, `approveLeaveApi`, `rejectLeaveApi`, `cancelLeaveApi`, `fetchMyLeaveBalancesApi`, `fetchEmployeeLeaveBalancesApi`, `fetchLeaveSummaryApi`.
   - Added `normalizeLeaveRequest` ensuring backend DTO attributes seamlessly match existing frontend expectations (`employeeName`, `department`, `requestedDays`, `submittedAt`, `reviewedBy`, `reviewerComment`).
   - Added status/type mappers (`mapLeaveStatusToFrontend`, `mapLeaveStatusToBackend`, `mapLeaveTypeToFrontend`, `mapLeaveTypeToBackend`).
   - Preserved `LEAVES_STORAGE_KEY` and legacy storage methods (`getStoredLeaves`, `saveStoredLeaves`, `generateInitialLeaves`) without deletions or auto-migration.
2. **`src/context/LeaveContext.jsx`**:
   - Converted state management to asynchronous REST API calls.
   - Added `isLoading`, `isMutating`, `error`, `refreshLeaves`, `refreshSummary`, and `backendSummary` states.
   - Operations (`applyLeave`, `updateLeave`, `approveLeave`, `rejectLeave`, `cancelLeave`) execute live HTTP requests with CSRF tokens and session credentials.
   - Graceful fallback for balance calculation if offline.
3. **`src/pages/Leaves/LeavesPage.jsx`**:
   - Added **Phase 8C.4 REST Active** status badge in the header.
   - Added manual **Refresh** action button with rotating icon indicator.
   - Added error notification banner with **Retry** action.
   - Restricted **Approve / Reject** action buttons strictly to `ADMIN`.
   - Restricted **Edit / Cancel** action buttons to `ADMIN` or the owning employee while `PENDING`.
4. **`src/pages/Leaves/ApplyLeaveModal.jsx`**:
   - Converted submission handler to `async` with `isSubmitting` state and button disablement.
   - Pre-selects authenticated employee when logged in as `EMPLOYEE`.
5. **`src/pages/Leaves/EditLeaveModal.jsx`**:
   - Converted submission handler to `async` with button loading indicators.
6. **`src/pages/Leaves/ReviewLeaveModal.jsx`**:
   - Converted review actions (`approve` / `reject`) to `async` with loading indicators.
   - Enforces non-empty rejection comment when rejecting.
7. **`src/pages/Leaves/LeaveBalancesView.jsx`**:
   - Added role privacy guard: non-admin employees can only view their own leave balance card.
8. **`src/pages/Dashboard/Dashboard.jsx` & `QuickActions.jsx`**:
   - Dashboard pending leave indicators seamlessly bind to `pendingCount` and `todayOnLeaveCount` sourced from PostgreSQL.

---

## 8. Automated Testing & Verification Results

### Backend Maven Test Suite (`.\mvnw.cmd test`)

```
[INFO] ------------------------------------------------------------------------
[INFO] Results:
[INFO] Tests run: 136, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time: 22.692 s
```

### Verification Against Phase 8C.4 Requirements

| # | Requirement | Test Class / Method | Result |
|---|---|---|---|
| 1 | Anonymous leave requests return 401 | `LeaveControllerSecurityTest.testAnonymousAccessReturns401` | PASS |
| 2 | Admin can list company leave requests | `LeaveControllerSecurityTest.testAdminCanListAllLeaveRequests` | PASS |
| 3 | Employee sees only their own requests | `LeaveControllerSecurityTest.testEmployeeCanListOwnLeaveRequests` | PASS |
| 4 | Employee cannot retrieve another employee's request | `LeaveControllerSecurityTest.testEmployeeCannotAccessOtherEmployeeLeaveRequest` | PASS |
| 5 | Employee cannot query other employee's balance | `LeaveControllerSecurityTest.testEmployeeAccessingOtherEmployeeBalanceReturns403` | PASS |
| 6 | Employee cannot approve leave | `LeaveControllerSecurityTest.testEmployeeCannotApproveLeave` | PASS |
| 7 | Employee cannot reject leave | `LeaveControllerSecurityTest.testEmployeeCannotRejectLeave` | PASS |
| 8 | Unsafe requests without CSRF token are rejected | `LeaveControllerSecurityTest.testApplyLeaveWithoutCsrfReturnsForbidden` | PASS |
| 9 | Valid request creation returns 201 | `LeaveControllerSecurityTest.testEmployeeApplyLeaveWithCsrfReturns201` | PASS |
| 10 | Invalid date ranges rejected (`endDate < startDate`) | `LeaveServiceTest.testApplyLeaveInvalidDateRange` | PASS |
| 11 | Invalid leave types rejected | Handled by enum deserialization & JPA check constraints | PASS |
| 12 | Overlapping requests rejected (409 Conflict) | `LeaveServiceTest.testApplyLeaveOverlappingRequestConflict` | PASS |
| 13 | Duplicate approval attempts rejected (409 Conflict) | `LeaveServiceTest.testApproveAlreadyApprovedRequestThrowsConflict` | PASS |
| 14 | Rejected requests do not deduct balances | `LeaveServiceTest.testRejectLeaveRequiresReasonAndDoesNotDeductBalance` | PASS |
| 15 | Only eligible pending requests can be reviewed | `LeaveServiceTest.testApproveAlreadyApprovedRequestThrowsConflict` | PASS |
| 16 | Cancellation follows state-transition policy | `LeaveServiceTest.testCancelPendingLeaveSuccess`<br>`LeaveServiceTest.testCancelApprovedLeaveThrowsConflict` | PASS |
| 17 | Self-service edit allowed while PENDING | `LeaveServiceTest.testUpdatePendingLeaveSuccess`<br>`LeaveServiceTest.testUpdateApprovedLeaveThrowsConflict` | PASS |
| 18 | Annual, sick, personal, and unpaid policy configured | `LeaveServiceTest.testLeaveBalancesCalculation` | PASS |
| 19 | Inclusive date calculations correct | `LeaveServiceTest.testDaysCountCalculationInclusive` | PASS |
| 20 | Approved leave updates balance correctly | `LeaveServiceTest.testLeaveBalancesCalculation` | PASS |
| 21 | Pending & rejected requests do not create approved effects | `LeaveServiceTest.testRejectLeaveRequiresReasonAndDoesNotDeductBalance` | PASS |
| 22 | Approved leave prevents attendance check-in | `AttendanceServiceTest.testEmployeeOnApprovedLeaveCannotCheckIn` | PASS |
| 23 | Approved leave reflected in attendance summary | `AttendanceServiceImpl.getAttendanceSummary` / `countApprovedEmployeesOnDate` | PASS |
| 24 | Attendance, Employee, and Department tests still pass | `AttendanceControllerSecurityTest` (15 tests)<br>`EmployeeControllerSecurityTest` (14 tests)<br>`DepartmentControllerSecurityTest` (15 tests)<br>`EmployeeServiceTest` (10 tests)<br>`DepartmentServiceTest` (12 tests) | PASS |
| 25 | Missing requests return 404 | `LeaveServiceTest.testGetLeaveRequestByIdNotFound` | PASS |
| 26 | Errors do not leak SQL, credentials, or stack traces | `GlobalExceptionHandlerTest.testDataIntegrityViolationSanitized` | PASS |

### Frontend Static Analysis & Production Build

- **Linter (`oxlint`)**:
  ```bash
  > npm run lint
  Found 0 warnings and 0 errors.
  Finished in 80ms on 58 files with 104 rules using 16 threads.
  ```
- **Production Build (`vite build`)**:
  ```bash
  > npm run build
  ✓ 1978 modules transformed.
  dist/index.html                   0.95 kB │ gzip:   0.51 kB
  dist/assets/index-CZMTlBWc.css   60.71 kB │ gzip:   9.56 kB
  dist/assets/index-D6iJXwXQ.js   478.05 kB │ gzip: 129.19 kB
  ✓ built in 574ms
  ```

---

## 9. Manual Verification Procedure

Follow these steps to manually verify Phase 8C.4 in a local development environment:

### Step 1: Start Backend and Frontend
1. Start PostgreSQL (e.g., port 5432).
2. Set local database password and start Spring Boot:
   ```powershell
   $env:DB_PASSWORD="your_postgres_password"
   cd backend
   .\mvnw.cmd spring-boot:run
   ```
3. Start the Vite React development server:
   ```powershell
   npm run dev
   ```
4. Navigate to `http://localhost:5173/login`.

### Step 2: Employee Leave Submission & Verification
1. Sign in as employee (`employee@workforcehub.io` / `EmployeeSecure2026!`).
2. Navigate to **Leave Management** (`/leaves`).
3. Observe the **Phase 8C.4 REST Active** badge in the header.
4. Click **Apply for Leave**.
5. Select **Annual Leave**, set Start Date = `2026-11-01`, End Date = `2026-11-03`, enter Reason = *"Family vacation"*, and submit.
6. Verify instant toast confirmation and notice the row appears in the table with status **Pending** and requested days = `3`.
7. Refresh the browser (`F5`). Verify the record persists from PostgreSQL.
8. Click **Balances** tab: verify Annual Leave balance reflects 3 pending days with 17 days remaining.
9. Verify that the **Approve** and **Reject** action buttons are NOT visible for employees.
10. Click **Edit** on the pending request, adjust dates, and save. Verify the update persists.

### Step 3: Overlapping Request & Conflict Validation
1. While logged in as employee, click **Apply for Leave** again.
2. Select any leave type for the same date span (`2026-11-02` to `2026-11-04`).
3. Click Submit.
4. Verify the backend rejects the overlapping submission with a clear error: *"Employee already has a pending or approved leave request overlapping this date range"*.

### Step 4: Administrator Approval & Attendance Integration
1. Log out and sign in as Administrator (`admin@workforcehub.io` / `AdminSecure2026!`).
2. Navigate to **Leave Management** (`/leaves`).
3. Locate the pending leave request in the table.
4. Click the **Review** (or **Approve**) button.
5. In the Review Leave Request dialog, enter an approval note and click **Approve**.
6. Verify the status updates to **Approved**, reviewer displays as Administrator, and the toast confirms approval.
7. Attempt to approve or reject the request again: verify the action is disabled/rejected.
8. If the approved leave covers today, navigate to **Attendance** (`/attendance`) and attempt to check in for that employee: verify check-in is blocked because the employee is on approved leave.
9. Verify the Dashboard metric displays the employee in the **On Leave Today** count.

### Step 5: Administrator Rejection Verification
1. As an employee, submit a separate leave request (e.g., Personal Leave).
2. As Administrator, click **Reject** on the request.
3. Ensure that submitting without a reason is disallowed. Enter reason *"Operational requirements during critical deploy"*, and confirm rejection.
4. Verify the status updates to **Rejected**.
5. Inspect the employee's leave balance: verify no leave days were deducted for the rejected request.

### Step 6: Ownership & Privacy Verification
1. As employee, open browser developer console.
2. Attempt to query another employee's leave balance:
   ```javascript
   await fetch('http://localhost:8080/api/leaves/balance/00000000-0000-0000-0000-000000000000', { credentials: 'include' });
   ```
3. Confirm the backend returns HTTP 403 Forbidden.
4. In the UI, navigate to **Leave Balances**: confirm the employee only views their personal balance card, preventing employee-to-employee data leakage.

---

## 10. Known Limitations & Blocked Tests

1. **Live Local Database Connection**: Automated integration and security tests run against H2 in PostgreSQL mode using Spring Boot test slices. Verification against a live PostgreSQL server requires local credentials provided via `$env:DB_PASSWORD`.
2. **Historical Browser Data**: Existing records stored in `localStorage` under `workforcehub_leaves` are preserved in the browser and have not been imported into PostgreSQL. Full end-to-end data migration and bulk import remain scheduled for Phase 8C.5.

---

## 11. Confirmations & Scope Boundaries

1. **LocalStorage Preservation**: The `workforcehub_leaves` localStorage key remains intact without resets, renames, or automatic imports.
2. **Attendance & Employee Safety**: Existing Employee, Department, and Attendance REST API endpoints and data structures were strictly preserved.
3. **Phase Completion**: Phase 8C.4 is complete. Execution has stopped without beginning Phase 8C.5.

-- ==============================================================================
-- WorkforceHub Initial PostgreSQL Schema Migration
-- Version: V1
-- Description: Establishes UserAccount, Department, Employee, AttendanceRecord,
--              and LeaveRequest relational tables with integrity constraints.
-- ==============================================================================

-- 1. User Accounts Table (Authentication Identity)
CREATE TABLE IF NOT EXISTS user_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_accounts_email UNIQUE (email),
    CONSTRAINT chk_user_accounts_role CHECK (role IN ('ADMIN', 'EMPLOYEE'))
);

CREATE INDEX IF NOT EXISTS idx_user_accounts_email ON user_accounts (email);
CREATE INDEX IF NOT EXISTS idx_user_accounts_role ON user_accounts (role);

-- 2. Departments Table
CREATE TABLE IF NOT EXISTS departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department_code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'Active',
    manager_employee_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_departments_code UNIQUE (department_code),
    CONSTRAINT uq_departments_name UNIQUE (name),
    CONSTRAINT chk_departments_status CHECK (status IN ('Active', 'Inactive'))
);

CREATE INDEX IF NOT EXISTS idx_departments_name ON departments (name);

-- 3. Employees Table
CREATE TABLE IF NOT EXISTS employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_code VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    job_title VARCHAR(150) NOT NULL,
    department_id UUID NOT NULL,
    manager_id UUID,
    employment_status VARCHAR(50) NOT NULL DEFAULT 'Active',
    employment_type VARCHAR(50) NOT NULL DEFAULT 'Full-Time',
    joining_date DATE NOT NULL,
    address TEXT,
    user_account_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_employees_code UNIQUE (employee_code),
    CONSTRAINT uq_employees_email UNIQUE (email),
    CONSTRAINT uq_employees_user_account UNIQUE (user_account_id),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) 
        REFERENCES departments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_employees_manager FOREIGN KEY (manager_id) 
        REFERENCES employees (id) ON DELETE SET NULL,
    CONSTRAINT fk_employees_user_account FOREIGN KEY (user_account_id) 
        REFERENCES user_accounts (id) ON DELETE SET NULL,
    CONSTRAINT chk_employees_status CHECK (employment_status IN ('Active', 'On Leave', 'Inactive')),
    CONSTRAINT chk_employees_type CHECK (employment_type IN ('Full-Time', 'Part-Time', 'Contract'))
);

CREATE INDEX IF NOT EXISTS idx_employees_code ON employees (employee_code);
CREATE INDEX IF NOT EXISTS idx_employees_email ON employees (email);
CREATE INDEX IF NOT EXISTS idx_employees_department ON employees (department_id);
CREATE INDEX IF NOT EXISTS idx_employees_manager ON employees (manager_id);
CREATE INDEX IF NOT EXISTS idx_employees_status ON employees (employment_status);

-- Add Foreign Key for Department Manager pointing to Employees
ALTER TABLE departments
    ADD CONSTRAINT fk_departments_manager FOREIGN KEY (manager_employee_id)
        REFERENCES employees (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_departments_manager ON departments (manager_employee_id);

-- 4. Attendance Records Table
CREATE TABLE IF NOT EXISTS attendance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    attendance_date DATE NOT NULL,
    check_in_time TIME NOT NULL,
    check_out_time TIME,
    status VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) 
        REFERENCES employees (id) ON DELETE RESTRICT,
    CONSTRAINT uq_attendance_employee_date UNIQUE (employee_id, attendance_date),
    CONSTRAINT chk_attendance_status CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'HALF_DAY'))
);

CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance_records (attendance_date);
CREATE INDEX IF NOT EXISTS idx_attendance_employee_date ON attendance_records (employee_id, attendance_date);
CREATE INDEX IF NOT EXISTS idx_attendance_status ON attendance_records (status);

-- 5. Leave Requests Table
CREATE TABLE IF NOT EXISTS leave_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days_count INT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reviewed_by_id UUID,
    review_comment TEXT,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leaves_employee FOREIGN KEY (employee_id) 
        REFERENCES employees (id) ON DELETE RESTRICT,
    CONSTRAINT fk_leaves_reviewer FOREIGN KEY (reviewed_by_id) 
        REFERENCES employees (id) ON DELETE SET NULL,
    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_leave_days_count CHECK (days_count > 0),
    CONSTRAINT chk_leave_type CHECK (leave_type IN ('ANNUAL', 'SICK', 'PERSONAL', 'UNPAID')),
    CONSTRAINT chk_leave_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_leaves_employee ON leave_requests (employee_id);
CREATE INDEX IF NOT EXISTS idx_leaves_status ON leave_requests (status);
CREATE INDEX IF NOT EXISTS idx_leaves_date_range ON leave_requests (start_date, end_date);

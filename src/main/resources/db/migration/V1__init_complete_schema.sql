-- V1__init_complete_schema.sql
-- Initialization complete schema for Dashboard RH

-- ===== USERS TABLE =====
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(15),
    role VARCHAR(50) NOT NULL DEFAULT 'EMPLOYEE',
    
    -- Account status columns (from UserDetails/BaseEntity)
    enabled BOOLEAN DEFAULT TRUE,
    account_non_expired BOOLEAN DEFAULT TRUE,
    account_non_locked BOOLEAN DEFAULT TRUE,
    credentials_non_expired BOOLEAN DEFAULT TRUE,
    
    -- Additional columns
    requires_time_tracking BOOLEAN DEFAULT TRUE,
    last_login_date DATETIME,
    password_last_changed DATETIME,
    email_verified_at DATETIME,
    
    -- Relations
    department_id BIGINT,
    manager_id BIGINT,
    
    -- Audit columns
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_role (role)
);

-- ===== DEPARTMENTS TABLE =====
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    manager_id BIGINT,
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    INDEX idx_name (name)
);

-- ===== EMPLOYEES TABLE =====
CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    department_id BIGINT,
    employee_number VARCHAR(50) UNIQUE,
    hire_date DATE,
    position VARCHAR(100),
    salary_grade VARCHAR(50),
    phone VARCHAR(20),
    address TEXT,
    emergency_contact VARCHAR(100),
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    INDEX idx_employee_number (employee_number),
    INDEX idx_department (department_id)
);

-- ===== LEAVE REQUESTS TABLE =====
CREATE TABLE leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') DEFAULT 'PENDING',
    approved_by_id BIGINT,
    approval_date DATETIME,
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    CONSTRAINT fk_leave_requests_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_requests_approver FOREIGN KEY (approved_by_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_employee (employee_id),
    INDEX idx_status (status),
    INDEX idx_start_date (start_date)
);

-- ===== ANNOUNCEMENTS TABLE =====
CREATE TABLE announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(50),
    priority ENUM('LOW', 'MEDIUM', 'HIGH') DEFAULT 'MEDIUM',
    start_date DATE NOT NULL,
    end_date DATE,
    published BOOLEAN DEFAULT FALSE,
    
    created_by_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    CONSTRAINT fk_announcements_creator FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_published (published),
    INDEX idx_start_date (start_date),
    INDEX idx_category (category)
);

-- ===== TASKS TABLE =====
CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    assigned_to_id BIGINT NOT NULL,
    created_by_id BIGINT NOT NULL,
    status ENUM('TODO', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'TODO',
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM',
    due_date DATE,
    completion_date DATETIME,
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    CONSTRAINT fk_tasks_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_assigned_to (assigned_to_id),
    INDEX idx_due_date (due_date)
);

-- ===== REPORTS TABLE =====
CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50),
    submitted_by_id BIGINT NOT NULL,
    submitted_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    report_date DATE,
    file_path VARCHAR(500),
    status ENUM('SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED') DEFAULT 'SUBMITTED',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    CONSTRAINT fk_reports_submitted_by FOREIGN KEY (submitted_by_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_submitted_by (submitted_by_id),
    INDEX idx_submitted_date (submitted_date)
);

-- ===== NOTIFICATIONS TABLE =====
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    message TEXT NOT NULL,
    type VARCHAR(50),
    related_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
);

-- ===== ATTENDANCE TABLE =====
CREATE TABLE attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    clock_in DATETIME,
    clock_out DATETIME,
    break_start DATETIME,
    break_end DATETIME,
    hours_worked DECIMAL(5, 2),
    break_duration DECIMAL(5, 2),
    overtime_hours DECIMAL(5, 2) DEFAULT 0.00,
    status VARCHAR(50) DEFAULT 'PRESENT',
    location VARCHAR(255),
    ip_address VARCHAR(50),
    device_info VARCHAR(500),
    notes TEXT,
    approved_by_id BIGINT,
    approval_date DATETIME,
    approved BOOLEAN DEFAULT FALSE,
    is_remote BOOLEAN DEFAULT FALSE,
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    CONSTRAINT fk_attendance_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_approver FOREIGN KEY (approved_by_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_date (date),
    INDEX idx_status (status),
    UNIQUE KEY uk_user_date (user_id, date)
);

-- ===== INSERT TEST DATA =====

-- Insert test users
INSERT INTO users (username, email, password, first_name, last_name, role, enabled, account_non_expired, account_non_locked, credentials_non_expired, requires_time_tracking) VALUES
('admin', 'admin@dashboard.com', '$2a$10$SlYQmyNdGzin7olVN3p5Be7DlH.PKZbv5H8KnzzVgXXbVxzy990qm', 'Admin', 'User', 'ADMIN', TRUE, TRUE, TRUE, TRUE, FALSE),
('manager', 'manager@dashboard.com', '$2a$10$SlYQmyNdGzin7olVN3p5Be7DlH.PKZbv5H8KnzzVgXXbVxzy990qm', 'Manager', 'User', 'MANAGER', TRUE, TRUE, TRUE, TRUE, TRUE),
('employee', 'employee@dashboard.com', '$2a$10$SlYQmyNdGzin7olVN3p5Be7DlH.PKZbv5H8KnzzVgXXbVxzy990qm', 'Employee', 'User', 'EMPLOYEE', TRUE, TRUE, TRUE, TRUE, TRUE);

-- Insert test departments
INSERT INTO departments (name, description, manager_id) VALUES
('IT', 'Information Technology Department', 2),
('HR', 'Human Resources Department', 2),
('Sales', 'Sales Department', 2);

-- Insert test employees
INSERT INTO employees (user_id, department_id, employee_number, hire_date, position, phone) VALUES
(3, 1, 'EMP001', '2023-01-15', 'Software Engineer', '0123456789'),
(2, 2, 'EMP002', '2022-06-01', 'HR Manager', '0123456790');


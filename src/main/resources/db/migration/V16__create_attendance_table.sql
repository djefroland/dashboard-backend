-- src/main/resources/db/migration/V16__create_attendance_table.sql

CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    clock_in DATETIME NULL,
    clock_out DATETIME NULL,
    break_start DATETIME NULL,
    break_end DATETIME NULL,
    hours_worked DECIMAL(5,2) NULL,
    break_duration DECIMAL(5,2) NULL,
    overtime_hours DECIMAL(5,2) DEFAULT 0,
    status ENUM('PRESENT', 'ABSENT', 'LATE', 'HALF_DAY', 'REMOTE', 'SICK_LEAVE', 'VACATION', 'BUSINESS_TRIP', 'TRAINING', 'OTHER') DEFAULT 'PRESENT',
    location VARCHAR(255) NULL,
    ip_address VARCHAR(45) NULL,
    device_info TEXT NULL,
    notes TEXT NULL,
    approved_by_id BIGINT NULL,
    approval_date DATETIME NULL,
    approved BOOLEAN DEFAULT FALSE,
    is_remote BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Contraintes
    UNIQUE KEY unique_user_date (user_id, date),
    
    -- Index pour les performances
    INDEX idx_attendance_user_date (user_id, date),
    INDEX idx_attendance_status (status),
    INDEX idx_attendance_approved (approved),
    INDEX idx_attendance_overtime (overtime_hours),
    INDEX idx_attendance_date_range (date, user_id),
    
    -- Clés étrangères
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (approved_by_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Données de test (optionnel)
INSERT INTO attendance (user_id, date, clock_in, clock_out, hours_worked, status, approved) VALUES
(1, CURDATE() - INTERVAL 1 DAY, CONCAT(CURDATE() - INTERVAL 1 DAY, ' 09:00:00'), CONCAT(CURDATE() - INTERVAL 1 DAY, ' 17:30:00'), 8.5, 'PRESENT', TRUE),
(2, CURDATE() - INTERVAL 1 DAY, CONCAT(CURDATE() - INTERVAL 1 DAY, ' 09:15:00'), CONCAT(CURDATE() - INTERVAL 1 DAY, ' 17:30:00'), 8.25, 'LATE', TRUE);

-- Permissions pour les rôles
-- Ces données peuvent être utiles si j 'ai besoin d'une table de permissions
-- INSERT INTO role_permissions (role, resource, action) VALUES
-- ('HR', 'attendance', 'view_all'),
-- ('HR', 'attendance', 'approve'),
-- ('TEAM_LEADER', 'attendance', 'view_team'),
-- ('TEAM_LEADER', 'attendance', 'approve_team'),
-- ('EMPLOYEE', 'attendance', 'view_own'),
-- ('INTERN', 'attendance', 'view_own');
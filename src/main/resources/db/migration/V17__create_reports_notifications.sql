-- src/main/resources/db/migration/V17__create_reports_notifications.sql

-- Table des rapports
CREATE TABLE IF NOT EXISTS reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    report_type ENUM('ATTENDANCE', 'LEAVE', 'PERFORMANCE', 'EMPLOYEE', 'DEPARTMENT', 'PAYROLL', 'ABSENCE', 'OVERTIME', 'CUSTOM') NOT NULL,
    description TEXT,
    author_id BIGINT NOT NULL,
    parameters JSON,
    start_date DATE,
    end_date DATE,
    department_id BIGINT,
    employee_id BIGINT,
    user_id BIGINT,
    report_path VARCHAR(500),
    report_format ENUM('PDF', 'EXCEL', 'CSV', 'JSON', 'HTML') DEFAULT 'PDF',
    file_size BIGINT,
    status ENUM('PENDING', 'GENERATING', 'COMPLETED', 'FAILED', 'EXPIRED') DEFAULT 'PENDING',
    generation_date DATE,
    expiry_date DATE,
    is_public BOOLEAN DEFAULT FALSE,
    access_level ENUM('AUTHOR_ONLY', 'DEPARTMENT', 'MANAGEMENT', 'HR_ONLY', 'DIRECTOR_ONLY', 'ALL') DEFAULT 'AUTHOR_ONLY',
    download_count INT DEFAULT 0,
    report_data LONGTEXT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Index
    INDEX idx_reports_author (author_id),
    INDEX idx_reports_type (report_type),
    INDEX idx_reports_status (status),
    INDEX idx_reports_public (is_public),
    INDEX idx_reports_dates (start_date, end_date),
    INDEX idx_reports_generation (generation_date),
    
    -- Clés étrangères
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Table des notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    sender_id BIGINT,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    notification_type ENUM('INFO', 'SUCCESS', 'WARNING', 'ERROR', 'LEAVE_REQUEST', 'LEAVE_APPROVED', 'LEAVE_REJECTED', 'ATTENDANCE_ALERT', 'OVERTIME_ALERT', 'REPORT_READY', 'USER_REQUEST', 'SYSTEM') DEFAULT 'INFO',
    priority ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT') DEFAULT 'NORMAL',
    status ENUM('UNREAD', 'READ', 'ARCHIVED', 'DELETED') DEFAULT 'UNREAD',
    read_at DATETIME,
    action_url VARCHAR(500),
    action_label VARCHAR(100),
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    metadata JSON,
    email_sent BOOLEAN DEFAULT FALSE,
    push_sent BOOLEAN DEFAULT FALSE,
    scheduled_for DATETIME,
    expires_at DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Index
    INDEX idx_notifications_recipient (recipient_id),
    INDEX idx_notifications_sender (sender_id),
    INDEX idx_notifications_status (status),
    INDEX idx_notifications_type (notification_type),
    INDEX idx_notifications_priority (priority),
    INDEX idx_notifications_created (created_at),
    INDEX idx_notifications_scheduled (scheduled_for),
    INDEX idx_notifications_expires (expires_at),
    
    -- Clés étrangères
    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Données de test
INSERT INTO reports (title, report_type, author_id, start_date, end_date, status, is_public) VALUES
('Rapport mensuel des présences', 'ATTENDANCE', 1, '2024-01-01', '2024-01-31', 'COMPLETED', TRUE),
('Analyse des congés Q1', 'LEAVE', 1, '2024-01-01', '2024-03-31', 'COMPLETED', FALSE),
('État des effectifs', 'EMPLOYEE', 2, '2024-01-01', '2024-12-31', 'PENDING', TRUE);

INSERT INTO notifications (recipient_id, sender_id, title, message, notification_type, priority) VALUES
(2, 1, 'Bienvenue', 'Bienvenue sur le dashboard RH !', 'INFO', 'NORMAL'),
(3, 1, 'Rapport disponible', 'Votre rapport mensuel est prêt à être téléchargé', 'REPORT_READY', 'HIGH'),
(4, 2, 'Demande de congé', 'Nouvelle demande de congé en attente d\'approbation', 'LEAVE_REQUEST', 'HIGH');

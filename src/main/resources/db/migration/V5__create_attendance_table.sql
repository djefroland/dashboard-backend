-- V5__create_attendance_table.sql
-- Création de la table de présence

CREATE TABLE attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    clock_in DATETIME,
    clock_out DATETIME,
    hours_worked DECIMAL(5, 2),
    status ENUM('PRESENT', 'ABSENT', 'LATE', 'HALF_DAY', 'REMOTE') DEFAULT 'PRESENT',
    notes TEXT,
    
    -- Métadonnées
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Contraintes
    UNIQUE KEY user_date_unique (user_id, date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
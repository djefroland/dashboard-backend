-- V6__create_leave_requests_table.sql
-- Création de la table des demandes de congés

CREATE TABLE leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    return_date DATE NOT NULL,
    total_days DECIMAL(5, 2) NOT NULL,
    type ENUM('CONGE_PAYE', 'RTT', 'MALADIE', 'MATERNITE', 'PATERNITE', 'SANS_SOLDE', 'AUTRE') NOT NULL,
    reason TEXT,
    status ENUM('PENDING', 'APPROVED_FIRST_LEVEL', 'APPROVED', 'REJECTED', 'CANCELLED') DEFAULT 'PENDING',
    
    -- Approbations
    approved_by_team_leader_id BIGINT,
    team_leader_approval_date DATETIME,
    team_leader_comments TEXT,
    
    approved_by_hr_id BIGINT,
    hr_approval_date DATETIME,
    hr_comments TEXT,
    
    -- Métadonnées
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Contraintes
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (approved_by_team_leader_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (approved_by_hr_id) REFERENCES users(id) ON DELETE SET NULL
);
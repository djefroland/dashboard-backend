-- V9__create_tasks_table.sql
-- Création de la table des tâches

CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status ENUM('TODO', 'IN_PROGRESS', 'REVIEW', 'DONE', 'CANCELLED') DEFAULT 'TODO',
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM',
    
    -- Assignations
    assigned_to_id BIGINT,
    assigned_by_id BIGINT NOT NULL,
    department_id BIGINT,
    
    -- Échéances
    due_date DATETIME,
    estimated_hours DECIMAL(5, 2),
    actual_hours DECIMAL(5, 2),
    
    -- Suivi
    completion_percentage INT DEFAULT 0,
    start_date DATETIME,
    completion_date DATETIME,
    
    -- Métadonnées
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Contraintes
    FOREIGN KEY (assigned_to_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_by_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);
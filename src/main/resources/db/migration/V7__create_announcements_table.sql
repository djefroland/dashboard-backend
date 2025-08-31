-- V7__create_announcements_table.sql
-- Création de la table des annonces

CREATE TABLE announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM',
    target_audience ENUM('ALL', 'DIRECTORS', 'HR', 'TEAM_LEADERS', 'EMPLOYEES', 'INTERNS') DEFAULT 'ALL',
    department_id BIGINT,
    
    -- Gestion des fichiers
    attachment_path VARCHAR(255),
    attachment_name VARCHAR(255),
    attachment_type VARCHAR(100),
    
    -- Visibilité et diffusion
    publish_date DATETIME NOT NULL,
    expiry_date DATETIME,
    is_published BOOLEAN DEFAULT TRUE,
    is_pinned BOOLEAN DEFAULT FALSE,
    
    -- Métadonnées
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Contraintes
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);
-- V1__create_users_table.sql
-- Création de la table users principale

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(15),
    role ENUM('DIRECTOR', 'HR', 'TEAM_LEADER', 'EMPLOYEE', 'INTERN') NOT NULL,
    
    -- Statuts du compte
    enabled BOOLEAN DEFAULT TRUE,
    account_non_expired BOOLEAN DEFAULT TRUE,
    account_non_locked BOOLEAN DEFAULT TRUE,
    credentials_non_expired BOOLEAN DEFAULT TRUE,
    
    -- Propriétés spécifiques
    requires_time_tracking BOOLEAN DEFAULT TRUE,
    
    -- Relations
    department_id BIGINT,
    manager_id BIGINT,
    
    -- Dates importantes
    last_login_date DATETIME,
    password_last_changed DATETIME,
    email_verified_at DATETIME,
    
    -- Audit automatique
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    -- Index pour les performances
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_department (department_id),
    INDEX idx_manager (manager_id)
);

-- Insertion des données de test (utilisateurs par défaut)
INSERT INTO users (username, email, password, first_name, last_name, role, requires_time_tracking) VALUES
-- Directeur (mot de passe: director123)
('director', 'director@dashboard-rh.com', '$2a$10$8eUQhVuiuYTVGQQx9nVQmOzgQXqKvqXqGxXQcP5U2B9Uz8sGJ3NGa', 'Jean', 'Directeur', 'DIRECTOR', FALSE),

-- RH (mot de passe: hr123)
('rh_manager', 'rh@dashboard-rh.com', '$2a$10$8eUQhVuiuYTVGQQx9nVQmOzgQXqKvqXqGxXQcP5U2B9Uz8sGJ3NGa', 'Marie', 'Dubois', 'HR', TRUE),

-- Team Leader (mot de passe: tl123)
('team_leader', 'tl@dashboard-rh.com', '$2a$10$8eUQhVuiuYTVGQQx9nVQmOzgQXqKvqXqGxXQcP5U2B9Uz8sGJ3NGa', 'Pierre', 'Martin', 'TEAM_LEADER', TRUE),

-- Employé (mot de passe: emp123)
('employee', 'emp@dashboard-rh.com', '$2a$10$8eUQhVuiuYTVGQQx9nVQmOzgQXqKvqXqGxXQcP5U2B9Uz8sGJ3NGa', 'Sophie', 'Legrand', 'EMPLOYEE', TRUE),

-- Stagiaire (mot de passe: intern123)
('intern', 'intern@dashboard-rh.com', '$2a$10$8eUQhVuiuYTVGQQx9nVQmOzgQXqKvqXqGxXQcP5U2B9Uz8sGJ3NGa', 'Lucas', 'Petit', 'INTERN', TRUE);
-- V10__insert_default_data.sql
-- Insertion des données par défaut

-- Insertion des rôles
INSERT INTO roles (name, display_name, can_manage_employees, can_approve_leaves, can_view_global_stats, can_export_data)
VALUES 
('DIRECTOR', 'Directeur', true, true, true, true),
('HR', 'Ressources Humaines', true, true, true, true),
('TEAM_LEADER', 'Chef d''équipe', false, true, false, false),
('EMPLOYEE', 'Employé', false, false, false, false),
('INTERN', 'Stagiaire', false, false, false, false);

-- Insertion d'un utilisateur admin par défaut (mot de passe: admin)
INSERT INTO users (username, email, password, first_name, last_name, role, enabled)
VALUES ('admin', 'admin@dashboard.com', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Admin', 'System', 'DIRECTOR', true);

-- Création d'un département par défaut
INSERT INTO departments (name, description, manager_id)
VALUES ('Administration', 'Département d''administration du système', 1);

-- Création d'un profil employé pour l'admin
INSERT INTO employees (user_id, employee_id, hire_date, contract_type, job_title, department_id)
VALUES (1, 'ADM001', CURDATE(), 'CDI', 'Administrateur Système', 1);
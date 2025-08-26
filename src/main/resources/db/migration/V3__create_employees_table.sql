-- V3__create_employees_table.sql
-- Création de la table des employés

CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    employee_id VARCHAR(50) UNIQUE,
    hire_date DATE NOT NULL,
    contract_type ENUM('CDI', 'CDD', 'STAGE', 'ALTERNANCE', 'FREELANCE') NOT NULL,
    job_title VARCHAR(100) NOT NULL,
    department_id BIGINT,
    manager_id BIGINT,
    
    -- Informations personnelles
    address TEXT,
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'France',
    birth_date DATE,
    
    -- Informations professionnelles
    salary DECIMAL(10, 2),
    leave_days_remaining INT DEFAULT 25,
    
    -- Métadonnées
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Contraintes
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL
);
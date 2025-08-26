-- V8__create_reports_table.sql
-- Création de la table des rapports

CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    type ENUM('PERFORMANCE', 'ATTENDANCE', 'LEAVE', 'DEPARTMENT', 'FINANCIAL', 'CUSTOM') NOT NULL,
    description TEXT,
    author_id BIGINT NOT NULL,
    
    -- Paramètres du rapport
    parameters JSON,
    start_date DATE,
    end_date DATE,
    department_id BIGINT,
    employee_id BIGINT,
    
    -- Stockage du rapport
    report_path VARCHAR(255),
    report_format ENUM('PDF', 'EXCEL', 'CSV', 'JSON', 'HTML') DEFAULT 'PDF',
    
    -- Accès et partage
    is_public BOOLEAN DEFAULT FALSE,
    access_level ENUM('DIRECTOR_ONLY', 'HR_ONLY', 'MANAGEMENT', 'ALL') DEFAULT 'MANAGEMENT',
    
    -- Métadonnées
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Contraintes
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE SET NULL
);
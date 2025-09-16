-- src/main/resources/db/migration/V13__update_leave_requests_table.sql
-- Mise à jour de la table des demandes de congés avec le workflow complet

ALTER TABLE leave_requests ADD COLUMN employee_id VARCHAR(50) NULL;
-- La colonne return_date existe déjà, nous la supprimons de cette migration
ALTER TABLE leave_requests ADD COLUMN emergency_contact VARCHAR(255) NULL;
ALTER TABLE leave_requests ADD COLUMN replacement_person VARCHAR(255) NULL;
ALTER TABLE leave_requests ADD COLUMN handover_notes TEXT NULL;

-- Workflow d'approbation
ALTER TABLE leave_requests ADD COLUMN manager_approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') NULL;
ALTER TABLE leave_requests ADD COLUMN approved_by_manager_id BIGINT NULL;
ALTER TABLE leave_requests ADD COLUMN manager_approval_date DATETIME NULL;
ALTER TABLE leave_requests ADD COLUMN manager_comments TEXT NULL;

ALTER TABLE leave_requests ADD COLUMN hr_approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') NULL;
ALTER TABLE leave_requests ADD COLUMN approved_by_hr_id BIGINT NULL;
ALTER TABLE leave_requests ADD COLUMN hr_approval_date DATETIME NULL;
ALTER TABLE leave_requests ADD COLUMN hr_comments TEXT NULL;

ALTER TABLE leave_requests ADD COLUMN director_approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') NULL;
ALTER TABLE leave_requests ADD COLUMN approved_by_director_id BIGINT NULL;
ALTER TABLE leave_requests ADD COLUMN director_approval_date DATETIME NULL;
ALTER TABLE leave_requests ADD COLUMN director_comments TEXT NULL;

-- Métadonnées
ALTER TABLE leave_requests ADD COLUMN submitted_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE leave_requests ADD COLUMN final_approval_date DATETIME NULL;
ALTER TABLE leave_requests ADD COLUMN rejection_reason TEXT NULL;
ALTER TABLE leave_requests ADD COLUMN cancelled_date DATETIME NULL;
ALTER TABLE leave_requests ADD COLUMN cancel_reason TEXT NULL;

-- Flags
ALTER TABLE leave_requests ADD COLUMN requires_manager_approval BOOLEAN DEFAULT TRUE;
ALTER TABLE leave_requests ADD COLUMN requires_hr_approval BOOLEAN DEFAULT TRUE;
ALTER TABLE leave_requests ADD COLUMN requires_director_approval BOOLEAN DEFAULT FALSE;
ALTER TABLE leave_requests ADD COLUMN is_urgent BOOLEAN DEFAULT FALSE;

-- Mise à jour du type de congé
ALTER TABLE leave_requests MODIFY COLUMN type ENUM(
    'ANNUAL_LEAVE', 'RTT', 'SICK_LEAVE', 'MATERNITY_LEAVE', 'PATERNITY_LEAVE', 
    'FAMILY_EVENT', 'UNPAID_LEAVE', 'STUDY_LEAVE', 'COMPASSIONATE_LEAVE', 'OTHER'
) NOT NULL;

-- Mise à jour du statut
ALTER TABLE leave_requests MODIFY COLUMN status ENUM(
    'PENDING', 'MANAGER_APPROVED', 'HR_APPROVED', 'APPROVED', 'REJECTED', 'CANCELLED'
) NOT NULL DEFAULT 'PENDING';

-- Contraintes de clés étrangères
ALTER TABLE leave_requests ADD CONSTRAINT FK_leave_approved_by_manager 
    FOREIGN KEY (approved_by_manager_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE leave_requests ADD CONSTRAINT FK_leave_approved_by_hr 
    FOREIGN KEY (approved_by_hr_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE leave_requests ADD CONSTRAINT FK_leave_approved_by_director 
    FOREIGN KEY (approved_by_director_id) REFERENCES users(id) ON DELETE SET NULL;

-- Index pour les performances
CREATE INDEX idx_leave_status ON leave_requests(status);
CREATE INDEX idx_leave_type ON leave_requests(type);
CREATE INDEX idx_leave_dates ON leave_requests(start_date, end_date);
CREATE INDEX idx_leave_manager_approval ON leave_requests(manager_approval_status);
CREATE INDEX idx_leave_hr_approval ON leave_requests(hr_approval_status);
CREATE INDEX idx_leave_director_approval ON leave_requests(director_approval_status);
CREATE INDEX idx_leave_submitted_date ON leave_requests(submitted_date);
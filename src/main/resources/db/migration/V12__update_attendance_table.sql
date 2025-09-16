-- src/main/resources/db/migration/V12__update_attendance_table.sql
-- Mise à jour de la table attendance avec les nouvelles colonnes

ALTER TABLE attendance ADD COLUMN break_start DATETIME NULL;
ALTER TABLE attendance ADD COLUMN break_end DATETIME NULL;
ALTER TABLE attendance ADD COLUMN break_duration DECIMAL(5,2) NULL;
ALTER TABLE attendance ADD COLUMN overtime_hours DECIMAL(5,2) DEFAULT 0;
ALTER TABLE attendance ADD COLUMN location VARCHAR(255) NULL;
ALTER TABLE attendance ADD COLUMN ip_address VARCHAR(45) NULL;
ALTER TABLE attendance ADD COLUMN device_info TEXT NULL;
ALTER TABLE attendance ADD COLUMN approved_by_id BIGINT NULL;
ALTER TABLE attendance ADD COLUMN approval_date DATETIME NULL;
ALTER TABLE attendance ADD COLUMN approved BOOLEAN DEFAULT FALSE;
ALTER TABLE attendance ADD COLUMN is_remote BOOLEAN DEFAULT FALSE;

-- Ajout des contraintes de clés étrangères
ALTER TABLE attendance ADD CONSTRAINT FK_attendance_approved_by 
    FOREIGN KEY (approved_by_id) REFERENCES users(id) ON DELETE SET NULL;

-- Index pour les performances
CREATE INDEX idx_attendance_status ON attendance(status);
CREATE INDEX idx_attendance_approved ON attendance(approved);
CREATE INDEX idx_attendance_date_range ON attendance(date, user_id);
CREATE INDEX idx_attendance_overtime ON attendance(overtime_hours);
-- Reset_Flyway_History.sql
-- Script pour réinitialiser l'historique Flyway et les tables
-- À exécuter AVANT de redémarrer le backend

-- Stopper les checks de contraintes temporairement
SET FOREIGN_KEY_CHECKS = 0;

-- Supprimer toutes les tables créées
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS announcements;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS leave_requests;
DROP TABLE IF EXISTS user_requests;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS positions;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS users;

-- Supprimer l'historique Flyway
DROP TABLE IF EXISTS flyway_schema_history;

-- Réactiver les checks
SET FOREIGN_KEY_CHECKS = 1;

-- Vérification
SELECT 'Database reset successful - Flyway history cleared' AS Status;

-- Clean up failed Flyway migration
DELETE FROM dashboard_rh.flyway_schema_history WHERE version = 2;

-- Reset all tables
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS dashboard_rh.attendance;
DROP TABLE IF EXISTS dashboard_rh.announcements;
DROP TABLE IF EXISTS dashboard_rh.leave_requests;
DROP TABLE IF EXISTS dashboard_rh.tasks;
DROP TABLE IF EXISTS dashboard_rh.reports;
DROP TABLE IF EXISTS dashboard_rh.notifications;
DROP TABLE IF EXISTS dashboard_rh.employees;
DROP TABLE IF EXISTS dashboard_rh.departments;
DROP TABLE IF EXISTS dashboard_rh.users;
SET FOREIGN_KEY_CHECKS = 1;

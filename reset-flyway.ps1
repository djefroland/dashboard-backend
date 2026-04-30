# Reset Flyway History and clean database
# This script resets the Flyway migration history to allow fresh migrations

Write-Host "Resetting Flyway migration history..." -ForegroundColor Yellow

$dbHost = "localhost"
$dbUser = "root"
$dbPassword = "root"
$dbName = "dashboard_rh"

# SQL commands to reset Flyway
$sqlCommands = @"
-- Drop failed migration record from Flyway history
DELETE FROM flyway_schema_history WHERE version = 2;

-- Reset schema to empty (drop all tables)
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS announcements;
DROP TABLE IF EXISTS leave_requests;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;
"@

Write-Host "Connecting to MySQL at $dbHost..." -ForegroundColor Cyan
Write-Host "SQL to execute:" -ForegroundColor Cyan
Write-Host $sqlCommands

# Save SQL to temp file
$tempFile = "$env:TEMP\reset_flyway.sql"
$sqlCommands | Out-File -FilePath $tempFile -Encoding UTF8

Write-Host "`nExecuting SQL reset..." -ForegroundColor Cyan

# Try to execute with mysql client
try {
    # Use WMI to find MySQL installation
    $mysqlPath = (Get-ChildItem "C:\Program Files\MySQL" -ErrorAction SilentlyContinue | Where-Object { $_.Name -match "MySQL Server" } | Select-Object -First 1).FullName
    if ($mysqlPath) {
        $mysqlExe = Join-Path $mysqlPath "bin\mysql.exe"
        if (Test-Path $mysqlExe) {
            & $mysqlExe -h $dbHost -u $dbUser -p$dbPassword $dbName < $tempFile
            Write-Host "`nFlyway history cleaned successfully!" -ForegroundColor Green
        }
        else {
            Write-Host "MySQL client not found at $mysqlExe" -ForegroundColor Red
            Write-Host "Please manually execute this SQL against your database:" -ForegroundColor Yellow
            Write-Host $sqlCommands
        }
    }
    else {
        Write-Host "MySQL not found in Program Files" -ForegroundColor Red
        Write-Host "Please manually execute this SQL against your database:" -ForegroundColor Yellow
        Write-Host $sqlCommands
    }
}
catch {
    Write-Host "Error executing SQL: $_" -ForegroundColor Red
    Write-Host "Please manually execute this SQL against your database:" -ForegroundColor Yellow
    Write-Host $sqlCommands
}
finally {
    # Cleanup temp file
    if (Test-Path $tempFile) { Remove-Item $tempFile -Force }
}

Write-Host "`nDone! You can now restart the backend with: mvn spring-boot:run" -ForegroundColor Green

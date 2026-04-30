# reset-database-complete.ps1
# Script pour réinitialiser complètement la base de données et redémarrer le backend

Write-Host "=================================" -ForegroundColor Cyan
Write-Host "🔄 RÉINITIALISATION COMPLÈTE DE LA BASE DE DONNÉES" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan

# 1. Exécuter le script SQL de reset
Write-Host "`n📊 Étape 1 : Suppression des tables et historique Flyway..." -ForegroundColor Yellow

# Vérifier que MySQL est accessible
$mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path $mysqlPath)) {
    $mysqlPath = "mysql"  # Essayer avec la commande globale
}

# Exécuter le script SQL
try {
    & $mysqlPath -h localhost -u root -p$env:MYSQL_PASSWORD dashboard_rh < Reset_Flyway_History.sql 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Tables supprimées avec succès" -ForegroundColor Green
    } else {
        Write-Host "⚠️  MySQL en ligne de commande non disponible, tentative via autre méthode..." -ForegroundColor Yellow
        Write-Host "   Veuillez exécuter manuellement dans MySQL Workbench ou MySQL CLI :" -ForegroundColor Yellow
        Write-Host "   SOURCE Reset_Flyway_History.sql;" -ForegroundColor Gray
        Write-Host ""
        Write-Host "   Puis relancez ce script." -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "⚠️  Impossible d'exécuter directement. Instructions manuelles :" -ForegroundColor Yellow
    Write-Host "   1. Ouvrir MySQL Workbench ou MySQL CLI" -ForegroundColor Gray
    Write-Host "   2. Se connecter à la base 'dashboard_rh'" -ForegroundColor Gray
    Write-Host "   3. Exécuter : SOURCE Reset_Flyway_History.sql;" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   Ou utiliser cette commande :" -ForegroundColor Gray
    Write-Host "   mysql -h localhost -u root dashboard_rh < Reset_Flyway_History.sql" -ForegroundColor Gray
    exit 1
}

# 2. Nettoyer le cache Maven/target
Write-Host "`n🧹 Étape 2 : Nettoyage du cache Maven..." -ForegroundColor Yellow
mvn clean -q
Write-Host "✅ Cache Maven nettoyé" -ForegroundColor Green

# 3. Compiler le projet
Write-Host "`n🔨 Étape 3 : Compilation du projet..." -ForegroundColor Yellow
mvn compile -q -DskipTests
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Compilation réussie" -ForegroundColor Green
} else {
    Write-Host "❌ Erreur de compilation" -ForegroundColor Red
    exit 1
}

# 4. Démarrer le backend
Write-Host "`n🚀 Étape 4 : Démarrage du backend..." -ForegroundColor Yellow
Write-Host "   Les migrations Flyway vont s'exécuter automatiquement..." -ForegroundColor Cyan
Write-Host ""

mvn spring-boot:run

Write-Host "`n=================================" -ForegroundColor Cyan
Write-Host "✅ PROCESSUS COMPLET TERMINÉ" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan

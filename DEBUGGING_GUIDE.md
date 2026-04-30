# 🔧 GUIDE DE DÉBOGAGE - Migration Flyway

## 🔴 Problème Détecté

**Erreur :** `La table 'notifications' existe déjà`

### Cause Racine

Les migrations avaient une **duplication de table** :
- **V4** crée les tables `notifications` et `reports` (version simple)
- **V7** tentait de créer à nouveau les mêmes tables (version améliorée)

Cela cause un **conflit lors de l'exécution de V7**.

---

## ✅ Solution Appliquée

### Changements Effectués

1. **V4** reste inchangée - crée les tables de base
2. **V7 a été transformée** en migration d'ALTER qui :
   - Ajoute les colonnes manquantes aux tables existantes
   - Ajoute les contraintes FK
   - Ajoute les index
   - Ne tente PAS de recréer les tables

### Migrations Maintenant

```
V1 → Création des tables core (users, departments, positions)
V2 → Création employee et user_requests
V3 → Création leave_requests
V4 → Création announcements, tasks, reports, notifications (simples)
V5 → Insertion des données de test
V6 → Création attendance
V7 → AMÉLIORATION - Ajoute colonnes à notifications et reports
V8 → Correction - Ajoute created_by et updated_by à attendance
```

---

## 🔄 Étapes de Redémarrage

### Option 1 : Automatique (Recommandée)

**Depuis le dossier `dashboard-backend` :**

```powershell
# Windows PowerShell
.\reset-database-complete.ps1
```

Cela va :
1. ✅ Réinitialiser la base de données
2. ✅ Nettoyer le cache Maven
3. ✅ Recompiler le projet
4. ✅ Démarrer le backend avec migrations

### Option 2 : Manuel (Pas à pas)

#### Étape 1 : Réinitialiser la Base de Données

Ouvrir **MySQL Workbench** ou **MySQL CLI** :

```sql
-- Se connecter à MySQL
mysql -h localhost -u root

-- Exécuter le script de reset
SOURCE Reset_Flyway_History.sql;
```

**Ou copier-coller dans MySQL Workbench :**

```sql
-- Stopper les checks de contraintes
SET FOREIGN_KEY_CHECKS = 0;

-- Supprimer toutes les tables
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
```

#### Étape 2 : Nettoyer le Cache Maven

```powershell
cd dashboard-backend
mvn clean
```

#### Étape 3 : Démarrer le Backend

```powershell
mvn spring-boot:run
```

Attendez les logs :
```
INFO ... Migrating schema `dashboard_rh` to version "1 - create core tables"
INFO ... Migrating schema `dashboard_rh` to version "2 - create employee and requests tables"
...
INFO ... Successfully applied 8 migrations
INFO ... Started DashboardBackendApplication
```

---

## 📊 Vérification du Démarrage

### Signaux Positifs ✅

```
Tomcat initialized with port 8080
HikariPool-1 - Start completed
Flyway : Successfully applied 8 migrations
Started DashboardBackendApplication in X seconds
```

### Signaux Négatifs ❌

```
Error creating bean
Migration failed
Unable to build Hibernate SessionFactory
Schema-validation: missing column
```

Si vous voyez des erreurs, consultez la section **Dépannage** ci-dessous.

---

## 🧪 Test Rapide

### 1. Backend Accessible ?

```powershell
curl http://localhost:8080/actuator/health
```

Réponse attendue : `{"status":"UP"}`

### 2. Base de Données ?

```powershell
# Terminal MySQL
mysql -u root dashboard_rh

# Vérifier les tables
SHOW TABLES;
SELECT * FROM flyway_schema_history;
```

### 3. Login Test ?

```powershell
$body = @{
    username = "director"
    password = "director123"
} | ConvertTo-Json

curl -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -Body $body
```

Réponse attendue : JWT Token

---

## 🐛 Dépannage

### Erreur : "La table 'X' existe déjà"

**Solution :**
```sql
-- Exécuter le reset complet
SOURCE Reset_Flyway_History.sql;
```

Puis redémarrer le backend.

### Erreur : "Missing column"

**Cause :** Ancien schéma en base de données.

**Solution :** Même reset (voir ci-dessus)

### Erreur : "Connection refused"

**Cause :** MySQL n'est pas en cours d'exécution.

**Solution :**
```powershell
# Windows
net start MySQL80

# Ou démarrer depuis Services
```

### Migration Échouée mais Enregistrée

**Cause :** Une migration échouée est marquée en BD, Flyway refuse de continuer.

**Solution :**
```sql
-- Voir l'historique
SELECT * FROM flyway_schema_history;

-- Marquer les échecs comme réparés (dangereux, à éviter)
-- Mieux : réinitialiser via Reset_Flyway_History.sql
```

---

## 📋 Checklist Finale

- [ ] MySQL en cours d'exécution
- [ ] Base `dashboard_rh` nettoyée (reset exécuté)
- [ ] Maven clean exécuté
- [ ] Backend démarré sans erreurs
- [ ] http://localhost:8080/actuator/health retourne UP
- [ ] Les 8 migrations listées en logs
- [ ] Création de user (director) possible
- [ ] Login retourne un JWT token

---

## 📞 Support

**Si le problème persiste :**

1. Vérifier les logs complets :
```powershell
mvn spring-boot:run > logs.txt 2>&1
cat logs.txt | Select-String "ERROR|WARN"
```

2. Vérifier que MySQL n'a pas d'autres sessions actives :
```sql
SHOW PROCESSLIST;
KILL [ID_SESSION];
```

3. Vérifier les permissions MySQL :
```sql
GRANT ALL PRIVILEGES ON dashboard_rh.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

---

**Dernière mise à jour :** 2026-04-30  
**Status :** ✅ Fixes Applied, Ready for Testing

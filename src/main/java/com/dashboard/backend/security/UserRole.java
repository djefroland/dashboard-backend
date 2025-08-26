package com.dashboard.backend.security;

/**
 * Énumération des rôles utilisateurs dans le système Dashboard RH
 * 
 * Basée sur le diagramme de cas d'utilisation :
 * - DIRECTOR : Accès total, pas de pointage
 * - HR : Gestion RH, approbation finale des congés
 * - TEAM_LEADER : Gestion d'équipe, première approbation des congés
 * - EMPLOYEE : Utilisateur standard, pointage requis
 * - INTERN : Stagiaire, mêmes droits que Employee + spécificités
 */
public enum UserRole {
    
    /**
     * Directeur - Niveau le plus élevé
     * - Accès à toutes les fonctionnalités
     * - Pas de pointage requis
     * - Peut valider les nouveaux employés
     * - Voit les stats globales
     */
    DIRECTOR("Directeur", 1, false),
    
    /**
     * Ressources Humaines
     * - Gestion complète des employés
     * - Approbation finale des congés
     * - Export des données
     * - Pointage optionnel selon configuration
     */
    HR("RH", 2, true),
    
    /**
     * Team Leader
     * - Gestion de son équipe
     * - Première approbation des congés
     * - Assignation des tâches
     * - Pointage requis
     */
    TEAM_LEADER("Team Leader", 3, true),
    
    /**
     * Employé standard
     * - Pointage requis
     * - Demande de congés
     * - Envoi de rapports
     * - Consultation des annonces
     */
    EMPLOYEE("Employé", 4, true),
    
    /**
     * Stagiaire
     * - Mêmes droits que Employee
     * - Fonctionnalités spécifiques aux stages
     * - Pointage requis
     */
    INTERN("Stagiaire", 5, true);

    private final String displayName;
    private final int hierarchyLevel;
    private final boolean requiresTimeTracking;

    UserRole(String displayName, int hierarchyLevel, boolean requiresTimeTracking) {
        this.displayName = displayName;
        this.hierarchyLevel = hierarchyLevel;
        this.requiresTimeTracking = requiresTimeTracking;
    }

    /**
     * Nom d'affichage du rôle
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Niveau hiérarchique (1 = plus élevé)
     */
    public int getHierarchyLevel() {
        return hierarchyLevel;
    }

    /**
     * Indique si ce rôle doit pointer ses heures
     */
    public boolean requiresTimeTracking() {
        return requiresTimeTracking;
    }

    /**
     * Vérifie si ce rôle est supérieur à un autre dans la hiérarchie
     */
    public boolean isHigherThan(UserRole other) {
        return this.hierarchyLevel < other.hierarchyLevel;
    }

    /**
     * Vérifie si ce rôle est un rôle de management
     */
    public boolean isManagerRole() {
        return this == DIRECTOR || this == HR || this == TEAM_LEADER;
    }

    /**
     * Vérifie si ce rôle peut approuver les congés
     */
    public boolean canApproveLeaves() {
        return this == DIRECTOR || this == HR || this == TEAM_LEADER;
    }

    /**
     * Vérifie si ce rôle peut créer/modifier des employés
     */
    public boolean canManageEmployees() {
        return this == DIRECTOR || this == HR;
    }

    /**
     * Vérifie si ce rôle peut voir les stats globales
     */
    public boolean canViewGlobalStats() {
        return this == DIRECTOR;
    }

    /**
     * Vérifie si ce rôle peut exporter des données
     */
    public boolean canExportData() {
        return this == DIRECTOR || this == HR;
    }

    /**
     * Retourne les rôles subordonnés à ce rôle
     */
    public UserRole[] getSubordinateRoles() {
        return switch (this) {
            case DIRECTOR -> new UserRole[]{HR, TEAM_LEADER, EMPLOYEE, INTERN};
            case HR -> new UserRole[]{TEAM_LEADER, EMPLOYEE, INTERN};
            case TEAM_LEADER -> new UserRole[]{EMPLOYEE, INTERN};
            case EMPLOYEE, INTERN -> new UserRole[]{};
        };
    }

    /**
     * Factory method pour créer un UserRole à partir d'une string
     */
    public static UserRole fromString(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Le rôle ne peut pas être null");
        }
        
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rôle invalide: " + role + 
                ". Rôles valides: DIRECTOR, HR, TEAM_LEADER, EMPLOYEE, INTERN");
        }
    }
}
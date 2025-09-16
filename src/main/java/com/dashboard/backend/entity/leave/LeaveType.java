// src/main/java/com/dashboard/backend/entity/leave/LeaveType.java
package com.dashboard.backend.entity.leave;

public enum LeaveType {
    ANNUAL_LEAVE("Congés payés", 25, true),
    RTT("Réduction du Temps de Travail", 12, true),
    SICK_LEAVE("Congé maladie", 365, false),
    MATERNITY_LEAVE("Congé maternité", 112, false),
    PATERNITY_LEAVE("Congé paternité", 25, false),
    FAMILY_EVENT("Événement familial", 5, false),
    UNPAID_LEAVE("Congé sans solde", 90, false),
    STUDY_LEAVE("Congé formation", 30, false),
    COMPASSIONATE_LEAVE("Congé compassionnel", 3, false),
    OTHER("Autre", 0, true);

    private final String displayName;
    private final int maxDaysPerYear;
    private final boolean requiresManagerApproval;

    LeaveType(String displayName, int maxDaysPerYear, boolean requiresManagerApproval) {
        this.displayName = displayName;
        this.maxDaysPerYear = maxDaysPerYear;
        this.requiresManagerApproval = requiresManagerApproval;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxDaysPerYear() {
        return maxDaysPerYear;
    }

    public boolean requiresManagerApproval() {
        return requiresManagerApproval;
    }
}
// src/main/java/com/dashboard/backend/entity/attendance/AttendanceStatus.java
package com.dashboard.backend.entity.attendance;

public enum AttendanceStatus {
    PRESENT("Présent"),
    ABSENT("Absent"),
    LATE("En retard"),
    HALF_DAY("Demi-journée"),
    REMOTE("Télétravail"),
    SICK_LEAVE("Congé maladie"),
    VACATION("Congé"),
    BUSINESS_TRIP("Déplacement professionnel"),
    TRAINING("Formation"),
    OTHER("Autre");

    private final String displayName;

    AttendanceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
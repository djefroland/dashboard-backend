// =============================================================================
// 9. Service de configuration des heures de travail
// =============================================================================

// src/main/java/com/dashboard/backend/config/WorkingHoursConfig.java
package com.dashboard.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@ConfigurationProperties(prefix = "attendance.working-hours")
@Data
public class WorkingHoursConfig {
    
    private String start = "09:00";
    private String end = "17:30";
    private int breakDuration = 60; // minutes
    
    public LocalTime getStartTime() {
        return LocalTime.parse(start);
    }
    
    public LocalTime getEndTime() {
        return LocalTime.parse(end);
    }
}
// src/main/java/com/dashboard/backend/scheduler/AttendanceScheduler.java
package com.dashboard.backend.scheduler;

import com.dashboard.backend.entity.attendance.Attendance;
import com.dashboard.backend.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttendanceScheduler {

    private final AttendanceRepository attendanceRepository;

    /**
     * Vérifie quotidiennement les présences incomplètes
     */
    @Scheduled(cron = "0 0 18 * * MON-FRI") // 18h00 du lundi au vendredi
    public void checkIncompleteAttendances() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Attendance> incompleteAttendances = attendanceRepository.findIncompleteAttendances(yesterday);
        
        if (!incompleteAttendances.isEmpty()) {
            log.warn("Présences incomplètes détectées pour le {}: {} enregistrements", 
                    yesterday, incompleteAttendances.size());
            
            // Ici, vous pourriez envoyer une notification aux RH
            // ou marquer automatiquement ces présences comme problématiques
        }
    }

    /**
     * Génère un rapport hebdomadaire des présences
     */
    @Scheduled(cron = "0 0 8 * * MON") // 8h00 tous les lundis
    public void generateWeeklyReport() {
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(6);
        
        log.info("Génération du rapport hebdomadaire des présences pour la période du {} au {}", 
                startDate, endDate);
        
        // Ici, vous pourriez générer et envoyer un rapport par email
    }
}
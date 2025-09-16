// src/main/java/com/dashboard/backend/entity/attendance/Attendance.java
package com.dashboard.backend.entity.attendance;

import com.dashboard.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}))
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "L'ID utilisateur est obligatoire")
    private Long userId;

    @Column(name = "date", nullable = false)
    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    @Column(name = "clock_in")
    private LocalDateTime clockIn;

    @Column(name = "clock_out")
    private LocalDateTime clockOut;

    @Column(name = "break_start")
    private LocalDateTime breakStart;

    @Column(name = "break_end")
    private LocalDateTime breakEnd;

    @Column(name = "hours_worked", precision = 5, scale = 2)
    private BigDecimal hoursWorked;

    @Column(name = "break_duration", precision = 5, scale = 2)
    private BigDecimal breakDuration;

    @Column(name = "overtime_hours", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(name = "location")
    private String location;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "approved_by_id")
    private Long approvedById;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Builder.Default
    private Boolean approved = false;

    @Builder.Default
    private Boolean isRemote = false;

    // Contrainte d'unicité : un seul enregistrement par utilisateur et par jour

    /**
     * Calcule automatiquement les heures travaillées
     */
    public void calculateHours() {
        if (clockIn != null && clockOut != null) {
            long totalMinutes = ChronoUnit.MINUTES.between(clockIn, clockOut);
            
            // Soustraire la pause si elle existe
            if (breakStart != null && breakEnd != null) {
                long breakMinutes = ChronoUnit.MINUTES.between(breakStart, breakEnd);
                this.breakDuration = BigDecimal.valueOf(breakMinutes / 60.0);
                totalMinutes -= breakMinutes;
            }
            
            this.hoursWorked = BigDecimal.valueOf(totalMinutes / 60.0);
            
            // Calculer les heures supplémentaires (au-delà de 8h)
            if (this.hoursWorked.compareTo(BigDecimal.valueOf(8)) > 0) {
                this.overtimeHours = this.hoursWorked.subtract(BigDecimal.valueOf(8));
            }
        }
    }

    /**
     * Vérifie si l'employé est en retard
     */
    public boolean isLate() {
        if (clockIn == null) return false;
        LocalTime standardStartTime = LocalTime.of(9, 0); // 9h00 par défaut
        return clockIn.toLocalTime().isAfter(standardStartTime);
    }

    /**
     * Vérifie si l'employé est parti tôt
     */
    public boolean isEarlyDeparture() {
        if (clockOut == null) return false;
        LocalTime standardEndTime = LocalTime.of(17, 30); // 17h30 par défaut
        return clockOut.toLocalTime().isBefore(standardEndTime);
    }
}
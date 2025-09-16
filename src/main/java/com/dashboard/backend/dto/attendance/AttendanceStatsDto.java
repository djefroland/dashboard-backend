// src/main/java/com/dashboard/backend/dto/attendance/AttendanceStatsDto.java
package com.dashboard.backend.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceStatsDto {
    private Long userId;
    private String userName;
    private String employeeId;
    
    private LocalDate periodStart;
    private LocalDate periodEnd;
    
    private Integer totalDays;
    private Integer presentDays;
    private Integer absentDays;
    private Integer lateDays;
    private Integer remoteDays;
    
    private BigDecimal totalHoursWorked;
    private BigDecimal averageHoursPerDay;
    private BigDecimal totalOvertimeHours;
    
    private Double attendanceRate; // Pourcentage de présence
    private Double punctualityRate; // Pourcentage de ponctualité
}
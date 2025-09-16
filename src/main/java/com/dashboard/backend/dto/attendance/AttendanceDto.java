// src/main/java/com/dashboard/backend/dto/attendance/AttendanceDto.java
package com.dashboard.backend.dto.attendance;

import com.dashboard.backend.entity.attendance.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDto {
    private Long id;
    private Long userId;
    private String userName;
    private String employeeId;
    private LocalDate date;
    
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private LocalDateTime breakStart;
    private LocalDateTime breakEnd;
    
    private BigDecimal hoursWorked;
    private BigDecimal breakDuration;
    private BigDecimal overtimeHours;
    
    private AttendanceStatus status;
    private String statusDisplayName;
    
    private String location;
    private String ipAddress;
    private String deviceInfo;
    private String notes;
    
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvalDate;
    private Boolean approved;
    private Boolean isRemote;
    
    // Propriétés calculées
    private Boolean isLate;
    private Boolean isEarlyDeparture;
    private Boolean isComplete; // clockIn ET clockOut
    private String workingTimeDisplay; // "8h30" par exemple
    private Boolean isOnBreak; // En pause (breakStart sans breakEnd)
}
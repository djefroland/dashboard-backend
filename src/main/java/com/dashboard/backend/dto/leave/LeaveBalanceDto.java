// src/main/java/com/dashboard/backend/dto/leave/LeaveBalanceDto.java
package com.dashboard.backend.dto.leave;

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
public class LeaveBalanceDto {
    private Long userId;
    private String userName;
    private String employeeId;
    
    private Integer annualLeaveEntitlement;
    private BigDecimal annualLeaveTaken;
    private BigDecimal annualLeaveRemaining;
    
    private Integer rttEntitlement;
    private BigDecimal rttTaken;
    private BigDecimal rttRemaining;
    
    private BigDecimal sickLeaveTaken;
    private BigDecimal otherLeaveTaken;
    
    private BigDecimal totalLeaveTaken;
    private BigDecimal totalLeaveRemaining;
    
    private LocalDate balanceAsOf;
    private Integer year;
}
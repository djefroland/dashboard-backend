// src/main/java/com/dashboard/backend/dto/leave/LeaveRequestDto.java
package com.dashboard.backend.dto.leave;

import com.dashboard.backend.entity.leave.LeaveRequest;
import com.dashboard.backend.entity.leave.LeaveType;
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
public class LeaveRequestDto {
    private Long id;
    private Long userId;
    private String userName;
    private String employeeId;
    
    private LeaveType leaveType;
    private String leaveTypeDisplayName;
    
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate returnDate;
    private BigDecimal totalDays;
    
    private String reason;
    private String emergencyContact;
    private String replacementPerson;
    private String handoverNotes;
    
    // Statut et workflow
    private LeaveRequest.LeaveStatus status;
    private String statusDisplayName;
    private LocalDateTime submittedDate;
    private LocalDateTime finalApprovalDate;
    
    // Approbations
    private LeaveRequest.ApprovalStatus managerApprovalStatus;
    private String managerApprovalStatusDisplay;
    private Long approvedByManagerId;
    private String approvedByManagerName;
    private LocalDateTime managerApprovalDate;
    private String managerComments;
    
    private LeaveRequest.ApprovalStatus hrApprovalStatus;
    private String hrApprovalStatusDisplay;
    private Long approvedByHrId;
    private String approvedByHrName;
    private LocalDateTime hrApprovalDate;
    private String hrComments;
    
    private LeaveRequest.ApprovalStatus directorApprovalStatus;
    private String directorApprovalStatusDisplay;
    private Long approvedByDirectorId;
    private String approvedByDirectorName;
    private LocalDateTime directorApprovalDate;
    private String directorComments;
    
    // Flags
    private Boolean requiresManagerApproval;
    private Boolean requiresHrApproval;
    private Boolean requiresDirectorApproval;
    private Boolean isUrgent;
    
    // Métadonnées de rejet/annulation
    private String rejectionReason;
    private LocalDateTime cancelledDate;
    private String cancelReason;
    
    // Propriétés calculées
    private Boolean canBeCancelled;
    private Boolean isActive;
    private Boolean isPending;
    private String nextApprover;
    private Integer daysUntilStart;
}
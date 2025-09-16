// src/main/java/com/dashboard/backend/entity/leave/LeaveRequest.java
package com.dashboard.backend.entity.leave;

import com.dashboard.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "L'ID utilisateur est obligatoire")
    private Long userId;

    @Column(name = "employee_id", nullable = false)
    private String employeeId; // Pour faciliter l'identification

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    @NotNull(message = "Le type de congé est obligatoire")
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate endDate;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate; // Premier jour de retour au travail

    @Column(name = "total_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalDays;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "replacement_person")
    private String replacementPerson;

    @Column(name = "handover_notes", columnDefinition = "TEXT")
    private String handoverNotes;

    // Workflow d'approbation
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private LeaveStatus status = LeaveStatus.PENDING;

    // Première approbation (Team Leader)
    @Column(name = "manager_approval_status")
    @Enumerated(EnumType.STRING)
    private ApprovalStatus managerApprovalStatus;

    @Column(name = "approved_by_manager_id")
    private Long approvedByManagerId;

    @Column(name = "manager_approval_date")
    private LocalDateTime managerApprovalDate;

    @Column(name = "manager_comments", columnDefinition = "TEXT")
    private String managerComments;

    // Approbation finale (RH)
    @Column(name = "hr_approval_status")
    @Enumerated(EnumType.STRING)
    private ApprovalStatus hrApprovalStatus;

    @Column(name = "approved_by_hr_id")
    private Long approvedByHrId;

    @Column(name = "hr_approval_date")
    private LocalDateTime hrApprovalDate;

    @Column(name = "hr_comments", columnDefinition = "TEXT")
    private String hrComments;

    // Approbation directeur (cas spéciaux)
    @Column(name = "director_approval_status")
    @Enumerated(EnumType.STRING)
    private ApprovalStatus directorApprovalStatus;

    @Column(name = "approved_by_director_id")
    private Long approvedByDirectorId;

    @Column(name = "director_approval_date")
    private LocalDateTime directorApprovalDate;

    @Column(name = "director_comments", columnDefinition = "TEXT")
    private String directorComments;

    // Métadonnées
    @Column(name = "submitted_date", nullable = false)
    @Builder.Default
    private LocalDateTime submittedDate = LocalDateTime.now();

    @Column(name = "final_approval_date")
    private LocalDateTime finalApprovalDate;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "cancelled_date")
    private LocalDateTime cancelledDate;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    // Flags
    @Builder.Default
    private Boolean requiresManagerApproval = true;

    @Builder.Default
    private Boolean requiresHrApproval = true;

    @Builder.Default
    private Boolean requiresDirectorApproval = false;

    @Builder.Default
    private Boolean isUrgent = false;

    /**
     * Énumération des statuts de demande de congé
     */
    public enum LeaveStatus {
        PENDING("En attente"),
        MANAGER_APPROVED("Approuvé par le manager"),
        HR_APPROVED("Approuvé par les RH"),
        APPROVED("Approuvé"),
        REJECTED("Rejeté"),
        CANCELLED("Annulé");

        private final String displayName;

        LeaveStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Énumération des statuts d'approbation
     */
    public enum ApprovalStatus {
        PENDING("En attente"),
        APPROVED("Approuvé"),
        REJECTED("Rejeté");

        private final String displayName;

        ApprovalStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Met à jour le statut global selon les approbations
     */
    public void updateOverallStatus() {
        if (this.managerApprovalStatus == ApprovalStatus.REJECTED || 
            this.hrApprovalStatus == ApprovalStatus.REJECTED ||
            this.directorApprovalStatus == ApprovalStatus.REJECTED) {
            this.status = LeaveStatus.REJECTED;
        } else if (this.requiresDirectorApproval && this.directorApprovalStatus == ApprovalStatus.APPROVED) {
            this.status = LeaveStatus.APPROVED;
            this.finalApprovalDate = LocalDateTime.now();
        } else if (!this.requiresDirectorApproval && this.hrApprovalStatus == ApprovalStatus.APPROVED) {
            this.status = LeaveStatus.APPROVED;
            this.finalApprovalDate = LocalDateTime.now();
        } else if (this.managerApprovalStatus == ApprovalStatus.APPROVED && !this.requiresHrApproval) {
            this.status = LeaveStatus.APPROVED;
            this.finalApprovalDate = LocalDateTime.now();
        } else if (this.managerApprovalStatus == ApprovalStatus.APPROVED && this.requiresHrApproval) {
            this.status = LeaveStatus.MANAGER_APPROVED;
        } else if (this.hrApprovalStatus == ApprovalStatus.APPROVED && this.requiresDirectorApproval) {
            this.status = LeaveStatus.HR_APPROVED;
        }
    }

    /**
     * Vérifie si la demande peut être annulée
     */
    public boolean canBeCancelled() {
        return this.status != LeaveStatus.CANCELLED && 
               this.status != LeaveStatus.REJECTED &&
               this.startDate.isAfter(LocalDate.now());
    }

    /**
     * Vérifie si la demande est en cours (dates actuelles)
     */
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return this.status == LeaveStatus.APPROVED && 
               !today.isBefore(this.startDate) && 
               !today.isAfter(this.endDate);
    }
}
// src/main/java/com/dashboard/backend/entity/employee/Employee.java
package com.dashboard.backend.entity.employee;

import com.dashboard.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "employee_id", unique = true)
    @NotBlank(message = "L'ID employé est obligatoire")
    private String employeeId;

    @Column(name = "hire_date", nullable = false)
    @NotNull(message = "La date d'embauche est obligatoire")
    private LocalDate hireDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false)
    private ContractType contractType;

    @Column(name = "job_title", nullable = false)
    @NotBlank(message = "Le titre du poste est obligatoire")
    private String jobTitle;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "position_id")
    private Long positionId;

    @Column(name = "manager_id")
    private Long managerId;

    // Informations personnelles
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    @Builder.Default
    private String country = "France";

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;
    
    @Pattern(regexp = "^[\\+]?[1-9]?[0-9]{7,15}$", message = "Le numéro d'urgence n'est pas valide")
    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;
    
    // Informations professionnelles
    @DecimalMin(value = "0.0", message = "Le salaire ne peut pas être négatif")
    @Column(name = "salary")
    private Double salary;
    
    @Min(value = 0, message = "Les jours de congé ne peuvent pas être négatifs")
    @Max(value = 50, message = "Les jours de congé ne peuvent pas dépasser 50")
    @Column(name = "leave_days_entitlement")
    @Builder.Default
    private Integer leaveDaysEntitlement = 25;
    
    @Column(name = "leave_days_taken")
    @Builder.Default
    private Integer leaveDaysTaken = 0;
    
    @Column(name = "leave_days_remaining")
    @Builder.Default
    private Integer leaveDaysRemaining = 25;
    
    // Informations stage (pour les stagiaires)
    @Column(name = "internship_start_date")
    private LocalDate internshipStartDate;
    
    @Column(name = "internship_end_date")
    private LocalDate internshipEndDate;
    
    @Column(name = "school_name")
    private String schoolName;
    
    @Column(name = "supervisor_id")
    private Long supervisorId;
    
    @Column(name = "internship_subject")
    private String internshipSubject;
    
    // Statut
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;
    
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;
    
    /**
     * Types de contrat
     */
    public enum ContractType {
        CDI("Contrat à Durée Indéterminée"),
        CDD("Contrat à Durée Déterminée"),
        STAGE("Stage"),
        ALTERNANCE("Alternance"),
        FREELANCE("Freelance"),
        CONSULTANT("Consultant");

        private final String displayName;

        ContractType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Statuts possibles d'un employé
     */
    public enum EmployeeStatus {
        ACTIVE,
        INACTIVE,
        TERMINATED,
        ON_LEAVE,
        PROBATION
    }

    /**
     * Vérifie si c'est un stagiaire
     */
    public boolean isIntern() {
        return contractType == ContractType.STAGE || contractType == ContractType.ALTERNANCE;
    }

    /**
     * Met à jour les jours de congé restants
     */
    public void updateRemainingLeaveDays() {
        this.leaveDaysRemaining = this.leaveDaysEntitlement - this.leaveDaysTaken;
    }
}
// src/main/java/com/dashboard/backend/dto/employee/UpdateEmployeeRequest.java
package com.dashboard.backend.dto.employee;

import com.dashboard.backend.entity.employee.Employee;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEmployeeRequest {
    
    @NotBlank(message = "Le titre du poste est obligatoire")
    @Size(max = 100, message = "Le titre du poste ne peut pas dépasser 100 caractères")
    private String jobTitle;
    
    private Long departmentId;
    private Long positionId;
    private Long managerId;
    
    // Informations personnelles
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private LocalDate birthDate;
    private String nationality;
    private String emergencyContactName;
    private String emergencyContactPhone;
    
    // Informations professionnelles
    @DecimalMin(value = "0.0", message = "Le salaire ne peut pas être négatif")
    private Double salary;
    
    @Min(value = 0, message = "Les jours de congé ne peuvent pas être négatifs")
    @Max(value = 50, message = "Les jours de congé ne peuvent pas dépasser 50")
    private Integer leaveDaysEntitlement;
    
    // Informations stage (pour les stagiaires)
    private LocalDate internshipStartDate;
    private LocalDate internshipEndDate;
    private String schoolName;
    private Long supervisorId;
    private String internshipSubject;
    
    @NotNull(message = "Le statut est obligatoire")
    private Employee.EmployeeStatus status;
}
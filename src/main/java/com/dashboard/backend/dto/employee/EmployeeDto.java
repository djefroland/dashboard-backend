// src/main/java/com/dashboard/backend/dto/employee/EmployeeDto.java
package com.dashboard.backend.dto.employee;

import com.dashboard.backend.entity.employee.Employee;
import com.dashboard.backend.security.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDto {
    private Long id;
    private Long userId;
    private String employeeId;
    
    // Informations utilisateur
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private UserRole role;
    
    // Informations emploi
    private LocalDate hireDate;
    private LocalDate endDate;
    private Employee.ContractType contractType;
    private String contractTypeDisplayName;
    private String jobTitle;
    
    // Organisation
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionTitle;
    private Long managerId;
    private String managerName;
    
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
    private Double salary;
    private Integer leaveDaysEntitlement;
    private Integer leaveDaysTaken;
    private Integer leaveDaysRemaining;
    
    // Informations stage (si stagiaire)
    private LocalDate internshipStartDate;
    private LocalDate internshipEndDate;
    private String schoolName;
    private Long supervisorId;
    private String supervisorName;
    private String internshipSubject;
    
    // Statut
    private Employee.EmployeeStatus status;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Flags utilitaires
    private Boolean isIntern;
    private Boolean canTakeLeave;
}
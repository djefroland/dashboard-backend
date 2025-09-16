// src/main/java/com/dashboard/backend/dto/employee/CreateEmployeeRequest.java
package com.dashboard.backend.dto.employee;

import com.dashboard.backend.entity.employee.Employee;
import com.dashboard.backend.security.UserRole;
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
public class CreateEmployeeRequest {
    
    // Informations utilisateur (si pas encore créé)
    @Size(max = 50, message = "Le nom d'utilisateur ne peut pas dépasser 50 caractères")
    private String username;
    
    @Email(message = "L'adresse email doit être valide")
    private String email;
    
    @Size(max = 50, message = "Le prénom ne peut pas dépasser 50 caractères")
    private String firstName;
    
    @Size(max = 50, message = "Le nom de famille ne peut pas dépasser 50 caractères")
    private String lastName;
    
    @Pattern(regexp = "^[\\+]?[1-9]?[0-9]{7,15}$", message = "Le numéro de téléphone n'est pas valide")
    private String phone;
    
    private UserRole role;
    
    // Ou ID utilisateur existant
    private Long userId;
    
    // Informations employé obligatoires
    @NotBlank(message = "L'ID employé est obligatoire")
    @Size(max = 50, message = "L'ID employé ne peut pas dépasser 50 caractères")
    private String employeeId;
    
    @NotNull(message = "La date d'embauche est obligatoire")
    @PastOrPresent(message = "La date d'embauche ne peut pas être dans le futur")
    private LocalDate hireDate;
    
    @NotNull(message = "Le type de contrat est obligatoire")
    private Employee.ContractType contractType;
    
    @NotBlank(message = "Le titre du poste est obligatoire")
    @Size(max = 100, message = "Le titre du poste ne peut pas dépasser 100 caractères")
    private String jobTitle;
    
    private Long departmentId;
    private Long positionId;
    private Long managerId;
    
    // Informations personnelles
    @Size(max = 500, message = "L'adresse ne peut pas dépasser 500 caractères")
    private String address;
    
    @Size(max = 100, message = "La ville ne peut pas dépasser 100 caractères")
    private String city;
    
    @Size(max = 20, message = "Le code postal ne peut pas dépasser 20 caractères")
    private String postalCode;
    
    @Size(max = 100, message = "Le pays ne peut pas dépasser 100 caractères")
    @Builder.Default
    private String country = "France";
    
    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate birthDate;
    
    @Size(max = 100, message = "La nationalité ne peut pas dépasser 100 caractères")
    private String nationality;
    
    @Size(max = 255, message = "Le nom du contact d'urgence ne peut pas dépasser 255 caractères")
    private String emergencyContactName;
    
    @Pattern(regexp = "^[\\+]?[1-9]?[0-9]{7,15}$", message = "Le numéro d'urgence n'est pas valide")
    private String emergencyContactPhone;
    
    // Informations professionnelles
    @DecimalMin(value = "0.0", message = "Le salaire ne peut pas être négatif")
    @DecimalMax(value = "1000000.0", message = "Le salaire ne peut pas dépasser 1 000 000")
    private Double salary;
    
    @Min(value = 0, message = "Les jours de congé ne peuvent pas être négatifs")
    @Max(value = 50, message = "Les jours de congé ne peuvent pas dépasser 50")
    @Builder.Default
    private Integer leaveDaysEntitlement = 25;
    
    // Informations stage (pour les stagiaires)
    private LocalDate internshipStartDate;
    
    @Future(message = "La date de fin de stage doit être dans le futur")
    private LocalDate internshipEndDate;
    
    @Size(max = 255, message = "Le nom de l'école ne peut pas dépasser 255 caractères")
    private String schoolName;
    
    private Long supervisorId;
    
    @Size(max = 500, message = "Le sujet de stage ne peut pas dépasser 500 caractères")
    private String internshipSubject;
    
    // Validation personnalisée pour les champs obligatoires selon le contexte
    @AssertTrue(message = "Les informations utilisateur ou un ID utilisateur existant sont obligatoires")
    private boolean isUserInfoValid() {
        if (userId != null) {
            return true; // Utilisateur existant
        }
        
        // Nouveau utilisateur : vérifier que les champs obligatoires sont présents
        return username != null && !username.trim().isEmpty() &&
               email != null && !email.trim().isEmpty() &&
               firstName != null && !firstName.trim().isEmpty() &&
               lastName != null && !lastName.trim().isEmpty();
    }
    
    @AssertTrue(message = "Les informations de stage sont obligatoires pour les stagiaires")
    private boolean isInternshipInfoValid() {
        if (contractType == Employee.ContractType.STAGE || contractType == Employee.ContractType.ALTERNANCE) {
            return internshipStartDate != null && internshipEndDate != null &&
                   schoolName != null && !schoolName.trim().isEmpty() &&
                   supervisorId != null;
        }
        return true; // Pas un stagiaire, validation OK
    }
    
    @AssertTrue(message = "La date de fin de stage doit être après la date de début")
    private boolean isInternshipDateRangeValid() {
        if (internshipStartDate != null && internshipEndDate != null) {
            return internshipEndDate.isAfter(internshipStartDate);
        }
        return true; // Pas de dates définies, validation OK
    }
}
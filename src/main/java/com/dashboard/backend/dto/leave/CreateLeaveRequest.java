// src/main/java/com/dashboard/backend/dto/leave/CreateLeaveRequest.java
package com.dashboard.backend.dto.leave;

import com.dashboard.backend.entity.leave.LeaveType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLeaveRequest {
    
    @NotNull(message = "Le type de congé est obligatoire")
    private LeaveType leaveType;
    
    @NotNull(message = "La date de début est obligatoire")
    @Future(message = "La date de début doit être dans le futur")
    private LocalDate startDate;
    
    @NotNull(message = "La date de fin est obligatoire")
    @Future(message = "La date de fin doit être dans le futur")
    private LocalDate endDate;
    
    @Size(max = 1000, message = "La raison ne peut pas dépasser 1000 caractères")
    private String reason;
    
    @Size(max = 255, message = "Le contact d'urgence ne peut pas dépasser 255 caractères")
    private String emergencyContact;
    
    @Size(max = 255, message = "La personne de remplacement ne peut pas dépasser 255 caractères")
    private String replacementPerson;
    
    @Size(max = 2000, message = "Les notes de passation ne peuvent pas dépasser 2000 caractères")
    private String handoverNotes;
    
    @Builder.Default
    private Boolean isUrgent = false;
}
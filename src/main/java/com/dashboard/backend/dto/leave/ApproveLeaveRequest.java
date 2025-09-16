// src/main/java/com/dashboard/backend/dto/leave/ApproveLeaveRequest.java
package com.dashboard.backend.dto.leave;

import com.dashboard.backend.entity.leave.LeaveRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApproveLeaveRequest {
    
    @NotNull(message = "La décision est obligatoire")
    private LeaveRequest.ApprovalStatus decision; // APPROVED ou REJECTED
    
    @Size(max = 1000, message = "Les commentaires ne peuvent pas dépasser 1000 caractères")
    private String comments;
    
    // Pour les cas spéciaux nécessitant l'approbation du directeur
    private Boolean escalateToDirector;
}
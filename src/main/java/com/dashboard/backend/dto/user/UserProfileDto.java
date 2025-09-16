// =============================================================================
// 1. DTO pour la gestion du profil utilisateur
// =============================================================================

// src/main/java/com/dashboard/backend/dto/user/UserProfileDto.java
package com.dashboard.backend.dto.user;

import com.dashboard.backend.security.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private UserRole role;
    private String roleDisplayName;
    
    // Informations département/manager
    private Long departmentId;
    private String departmentName;
    private Long managerId;
    private String managerName;
    
    // Statuts
    private Boolean enabled;
    private Boolean requiresTimeTracking;
    
    // Dates importantes
    private LocalDateTime lastLoginDate;
    private LocalDateTime createdAt;
    private LocalDateTime emailVerifiedAt;
    
    // Permissions
    private Boolean canManageEmployees;
    private Boolean canApproveLeaves;
    private Boolean canViewGlobalStats;
    private Boolean canExportData;
}
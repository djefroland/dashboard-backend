// src/main/java/com/dashboard/backend/dto/user/UserRequestDto.java
package com.dashboard.backend.dto.user;

import com.dashboard.backend.entity.user.UserRequest;
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
public class UserRequestDto {
    private Long id;
    private UserRequest.UserRequestType requestType;
    private UserRequest.UserRequestStatus status;
    
    // Informations utilisateur
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private UserRole requestedRole;
    private String requestedRoleDisplayName;
    
    // Informations organisationnelles
    private Long departmentId;
    private String departmentName;
    private Long managerId;
    private String managerName;
    
    // Workflow
    private Long requestedById;
    private String requestedByName;
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewDate;
    private String reviewComments;
    
    private String justification;
    private LocalDateTime createdAt;
    private Long userCreatedId;
}
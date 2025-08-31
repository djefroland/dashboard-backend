package com.dashboard.backend.dto.auth;

import com.dashboard.backend.security.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour la réponse de connexion réussie
 * Contient le token JWT et les informations utilisateur essentielles
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    // Token d'authentification
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn; // en secondes

    // Informations utilisateur
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private UserRole role;
    private String roleDisplayName;

    // Métadonnées de connexion
    private LocalDateTime loginTime;
    private Boolean firstLogin;
    private Boolean passwordExpired;
    private Boolean requiresTimeTracking;

    // Permissions principales
    private Boolean canManageEmployees;
    private Boolean canApproveLeaves;
    private Boolean canViewGlobalStats;
    private Boolean canExportData;

    /**
     * Factory method pour créer une LoginResponse à partir d'un User
     */
    public static LoginResponse fromUser(
            com.dashboard.backend.entity.user.User user, 
            String accessToken, 
            String refreshToken, 
            Long expiresIn) {
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole())
                .roleDisplayName(user.getRole().getDisplayName())
                .loginTime(LocalDateTime.now())
                .firstLogin(user.getLastLoginDate() == null)
                .passwordExpired(!user.isCredentialsNonExpired())
                .requiresTimeTracking(user.shouldTrackTime())
                .canManageEmployees(user.getRole().canManageEmployees())
                .canApproveLeaves(user.getRole().canApproveLeaves())
                .canViewGlobalStats(user.getRole().canViewGlobalStats())
                .canExportData(user.getRole().canExportData())
                .build();
    }
}
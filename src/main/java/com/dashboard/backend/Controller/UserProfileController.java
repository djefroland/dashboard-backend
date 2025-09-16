// src/main/java/com/dashboard/backend/Controller/UserProfileController.java
package com.dashboard.backend.Controller;

import com.dashboard.backend.dto.user.ChangePasswordRequest;
import com.dashboard.backend.dto.user.UpdateProfileRequest;
import com.dashboard.backend.dto.user.UserProfileDto;
import com.dashboard.backend.service.auth.JwtService;
import com.dashboard.backend.service.user.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Profil Utilisateur", description = "APIs pour la gestion du profil utilisateur")
@SecurityRequirement(name = "Bearer Authentication")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final JwtService jwtService;

    /**
     * Obtient le profil de l'utilisateur connecté
     */
    @GetMapping("/me")
    @Operation(summary = "Profil utilisateur actuel", 
               description = "Retourne les informations complètes de l'utilisateur connecté")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profil récupéré avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<UserProfileDto> getCurrentUserProfile(HttpServletRequest request) {
        Long userId = extractUserIdFromToken(request);
        
        UserProfileDto profile = userProfileService.getCurrentUserProfile(userId);
        
        log.info("Profil consulté par l'utilisateur: {} (ID: {})", 
                profile.getUsername(), profile.getId());
        
        return ResponseEntity.ok(profile);
    }

    /**
     * Met à jour le profil de l'utilisateur connecté
     */
    @PutMapping("/me")
    @Operation(summary = "Modifier le profil", 
               description = "Met à jour les informations personnelles de l'utilisateur connecté")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profil mis à jour avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<UserProfileDto> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserIdFromToken(httpRequest);
        
        UserProfileDto updatedProfile = userProfileService.updateProfile(userId, request);
        
        log.info("Profil mis à jour par l'utilisateur: {} (ID: {})", 
                updatedProfile.getUsername(), updatedProfile.getId());
        
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Change le mot de passe de l'utilisateur connecté
     */
    @PutMapping("/me/password")
    @Operation(summary = "Changer le mot de passe", 
               description = "Change le mot de passe de l'utilisateur connecté")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mot de passe changé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides ou ancien mot de passe incorrect"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserIdFromToken(httpRequest);
        
        userProfileService.changePassword(userId, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Mot de passe changé avec succès");
        response.put("timestamp", LocalDateTime.now());
        
        log.info("Mot de passe changé pour l'utilisateur ID: {}", userId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Supprime le compte de l'utilisateur (soft delete)
     */
    @DeleteMapping("/me")
    @Operation(summary = "Supprimer le compte", 
               description = "Désactive le compte de l'utilisateur connecté")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Compte désactivé avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Action non autorisée"),
        @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<Map<String, Object>> deactivateAccount(HttpServletRequest request) {
        Long userId = extractUserIdFromToken(request);
        
        // Note: Implémenter la logique de désactivation si nécessaire
        // userProfileService.deactivateAccount(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Fonctionnalité de désactivation à implémenter");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Extrait l'ID utilisateur du token JWT
     */
    private Long extractUserIdFromToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            throw new RuntimeException("Token JWT manquant");
        }
        return jwtService.extractUserId(token);
    }

    /**
     * Extrait le token JWT de la requête
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
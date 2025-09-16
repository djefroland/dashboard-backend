package com.dashboard.backend.Controller;

import com.dashboard.backend.dto.auth.LoginRequest;
import com.dashboard.backend.dto.auth.LoginResponse;
import com.dashboard.backend.service.auth.AuthService;
import com.dashboard.backend.service.auth.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

/**
 * Controller pour les opérations d'authentification
 * Gestion de la connexion, déconnexion et rafraîchissement des tokens
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentification", description = "APIs pour l'authentification des utilisateurs")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    /**
     * Connexion d'un utilisateur
     */
    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur", 
               description = "Authentifie un utilisateur avec username/email et mot de passe")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Connexion réussie"),
        @ApiResponse(responseCode = "401", description = "Identifiants incorrects"),
        @ApiResponse(responseCode = "403", description = "Compte désactivé ou verrouillé")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                             HttpServletRequest request) {
        
        log.info("Tentative de connexion pour: {} depuis IP: {}", 
                loginRequest.getIdentifier(), getClientIpAddress(request));

 structure
        LoginResponse response = authService.login(loginRequest, request);

        
        log.info("Connexion réussie pour: {} (Role: {})", 
                response.getUsername(), response.getRole());
        
        // Log détaillé de la réponse pour debug
        log.info("Contenu de la réponse: accessToken={}, refreshToken={}, username={}", 
                response.getAccessToken(), 
                response.getRefreshToken(), 
                response.getUsername());
                
        return ResponseEntity.ok(response);
    }

    /**
     * Rafraîchissement du token d'accès
     */
    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token", 
               description = "Génère un nouveau token d'accès à partir du refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token rafraîchi avec succès"),
        @ApiResponse(responseCode = "401", description = "Refresh token invalide ou expiré")
    })
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody Map<String, String> request) {
        
        String refreshToken = request.get("refreshToken");
        
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Demande de rafraîchissement de token");

        LoginResponse response = authService.refreshToken(refreshToken);
        
        log.info("Token rafraîchi pour: {}", response.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * Déconnexion d'un utilisateur
     */
    @PostMapping("/logout")
    @Operation(summary = "Déconnexion utilisateur", 
               description = "Déconnecte l'utilisateur (invalide le token côté client)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Déconnexion réussie"),
        @ApiResponse(responseCode = "401", description = "Token invalide")
    })
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        
        String token = extractTokenFromRequest(request);
        
        if (token != null) {
            authService.logout(token);
            
            String username = jwtService.extractUsername(token);
            log.info("Déconnexion de l'utilisateur: {}", username);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Déconnexion réussie");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Validation d'un token
     */
    @PostMapping("/validate")
    @Operation(summary = "Valider un token", 
               description = "Vérifie si un token JWT est valide")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token valide"),
        @ApiResponse(responseCode = "401", description = "Token invalide")
    })
    public ResponseEntity<Map<String, Object>> validateToken(HttpServletRequest request) {
        
        String token = extractTokenFromRequest(request);
        
        if (token == null) {
            return ResponseEntity.badRequest().build();
        }

        boolean isValid = authService.isTokenValid(token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        response.put("timestamp", LocalDateTime.now());

        if (isValid) {
            // Ajouter des informations supplémentaires si le token est valide
            Map<String, Object> userInfo = jwtService.extractUserInfo(token);
            response.put("userInfo", userInfo);
            response.put("expiresIn", jwtService.getExpirationTime(token));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Informations sur l'utilisateur connecté
     */
    @GetMapping("/me")
    @Operation(summary = "Profil utilisateur actuel", 
               description = "Retourne les informations de l'utilisateur connecté")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Informations utilisateur"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
        
        String token = extractTokenFromRequest(request);
        
        if (token == null || !authService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> userInfo = jwtService.extractUserInfo(token);
        userInfo.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Changement de mot de passe
     */
    @PostMapping("/change-password")
    @Operation(summary = "Changer le mot de passe", 
               description = "Change le mot de passe de l'utilisateur connecté")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mot de passe changé"),
        @ApiResponse(responseCode = "400", description = "Ancien mot de passe incorrect"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody Map<String, String> request, 
            HttpServletRequest httpRequest) {
        
        String token = extractTokenFromRequest(httpRequest);
        
        if (token == null || !authService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }

        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = jwtService.extractUserId(token);
        authService.changePassword(userId, oldPassword, newPassword);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Mot de passe changé avec succès");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
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

    /**
     * Obtient l'adresse IP du client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
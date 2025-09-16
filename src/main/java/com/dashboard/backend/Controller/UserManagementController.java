// src/main/java/com/dashboard/backend/Controller/UserManagementController.java
package com.dashboard.backend.Controller;

import com.dashboard.backend.dto.user.CreateUserRequest;
import com.dashboard.backend.dto.user.ReviewUserRequestDto;
import com.dashboard.backend.dto.user.UserRequestDto;
import com.dashboard.backend.entity.user.UserRequest;
import com.dashboard.backend.service.auth.JwtService;
import com.dashboard.backend.service.user.UserRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion Utilisateurs", description = "APIs pour la gestion des utilisateurs et des demandes")
@SecurityRequirement(name = "Bearer Authentication")
public class UserManagementController {

    private final UserRequestService userRequestService;
    private final JwtService jwtService;

    /**
     * Crée une demande de création d'utilisateur (RH seulement)
     */
    @PostMapping("/requests")
    @PreAuthorize("hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Créer une demande d'utilisateur", 
               description = "Crée une demande de création d'utilisateur qui doit être approuvée par le Directeur")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Demande créée avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - RH requis")
    })
    public ResponseEntity<UserRequestDto> createUserRequest(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {
        
        Long requesterId = extractUserIdFromToken(httpRequest);
        
        UserRequestDto createdRequest = userRequestService.createUserRequest(request, requesterId);
        
        log.info("Demande de création d'utilisateur créée: {} par l'utilisateur ID: {}", 
                request.getUsername(), requesterId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
    }

    /**
     * Obtient toutes les demandes en attente (Directeur seulement)
     */
    @GetMapping("/requests/pending")
    @PreAuthorize("hasRole('DIRECTOR')")
    @Operation(summary = "Lister les demandes en attente", 
               description = "Obtient toutes les demandes d'utilisateurs en attente d'approbation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des demandes récupérée"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - Directeur requis")
    })
    public ResponseEntity<List<UserRequestDto>> getPendingRequests() {
        List<UserRequestDto> pendingRequests = userRequestService.getPendingRequests();
        
        log.info("Consultation des demandes en attente - {} demande(s)", pendingRequests.size());
        
        return ResponseEntity.ok(pendingRequests);
    }

    /**
     * Obtient toutes les demandes avec pagination et filtre
     */
    @GetMapping("/requests")
    @PreAuthorize("hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Lister les demandes d'utilisateurs", 
               description = "Obtient les demandes avec pagination et filtre par statut")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste paginée des demandes"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<Page<UserRequestDto>> getRequests(
            @RequestParam(required = false) UserRequest.UserRequestStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<UserRequestDto> requests = userRequestService.getRequests(status, pageable);
        
        return ResponseEntity.ok(requests);
    }

    /**
     * Approuve ou rejette une demande (Directeur seulement)
     */
    @PutMapping("/requests/{requestId}/review")
    @PreAuthorize("hasRole('DIRECTOR')")
    @Operation(summary = "Examiner une demande", 
               description = "Approuve ou rejette une demande d'utilisateur")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Demande examinée avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides ou demande déjà traitée"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - Directeur requis"),
        @ApiResponse(responseCode = "404", description = "Demande non trouvée")
    })
    public ResponseEntity<UserRequestDto> reviewRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ReviewUserRequestDto reviewDto,
            HttpServletRequest httpRequest) {
        
        Long reviewerId = extractUserIdFromToken(httpRequest);
        
        UserRequestDto reviewedRequest = userRequestService.reviewRequest(requestId, reviewDto, reviewerId);
        
        log.info("Demande {} {} par le Directeur ID: {}", 
                requestId, reviewDto.getDecision(), reviewerId);
        
        return ResponseEntity.ok(reviewedRequest);
    }

    /**
     * Annule une demande
     */
    @DeleteMapping("/requests/{requestId}")
    @PreAuthorize("hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Annuler une demande", 
               description = "Annule une demande d'utilisateur en attente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Demande annulée avec succès"),
        @ApiResponse(responseCode = "400", description = "Demande ne peut pas être annulée"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Demande non trouvée")
    })
    public ResponseEntity<Map<String, Object>> cancelRequest(
            @PathVariable Long requestId,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserIdFromToken(httpRequest);
        
        userRequestService.cancelRequest(requestId, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Demande annulée avec succès");
        response.put("requestId", requestId);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Obtient les statistiques des demandes
     */
    @GetMapping("/requests/statistics")
    @PreAuthorize("hasRole('DIRECTOR')")
    @Operation(summary = "Statistiques des demandes", 
               description = "Obtient les statistiques des demandes d'utilisateurs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistiques récupérées"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - Directeur requis")
    })
    public ResponseEntity<Map<String, Object>> getRequestsStatistics() {
        Map<String, Object> statistics = userRequestService.getRequestsStatistics();
        statistics.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(statistics);
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
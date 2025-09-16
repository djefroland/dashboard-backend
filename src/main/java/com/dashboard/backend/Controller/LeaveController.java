// src/main/java/com/dashboard/backend/Controller/LeaveController.java
package com.dashboard.backend.Controller;

import com.dashboard.backend.dto.leave.*;
import com.dashboard.backend.service.auth.JwtService;
import com.dashboard.backend.service.leave.LeaveService;
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
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Congés", description = "APIs pour la gestion des demandes de congés et workflow d'approbation")
@SecurityRequirement(name = "Bearer Authentication")
public class LeaveController {

    private final LeaveService leaveService;
    private final JwtService jwtService;

    /**
     * Crée une nouvelle demande de congé
     */
    @PostMapping("/request")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('INTERN') or hasRole('TEAM_LEADER') or hasRole('HR')")
    @Operation(summary = "Créer une demande de congé", 
               description = "Crée une nouvelle demande de congé pour l'utilisateur connecté")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Demande créée avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides ou solde insuffisant"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Non autorisé")
    })
    public ResponseEntity<LeaveRequestDto> createLeaveRequest(
            @Valid @RequestBody CreateLeaveRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserIdFromToken(httpRequest);
        
        LeaveRequestDto leaveRequest = leaveService.createLeaveRequest(request, userId);
        
        log.info("Demande de congé créée: {} jours de {} par l'utilisateur ID: {}",
                leaveRequest.getTotalDays(), request.getLeaveType(), userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveRequest);
    }

    /**
     * Mes demandes de congé
     */
    @GetMapping("/my-requests")
    @Operation(summary = "Mes demandes de congé", 
               description = "Obtient toutes les demandes de congé de l'utilisateur connecté")
    public ResponseEntity<Page<LeaveRequestDto>> getMyLeaveRequests(
            @PageableDefault(size = 10, sort = "submittedDate", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserIdFromToken(httpRequest);
        
        Page<LeaveRequestDto> requests = leaveService.getUserLeaveRequests(userId, userId, pageable);
        
        return ResponseEntity.ok(requests);
    }

    /**
     * Mon solde de congés
     */
    @GetMapping("/my-balance")
    @Operation(summary = "Mon solde de congés", 
               description = "Obtient le solde de congés de l'utilisateur connecté")
    public ResponseEntity<LeaveBalanceDto> getMyLeaveBalance(
            @RequestParam(required = false) Integer year,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserIdFromToken(httpRequest);
        
        LeaveBalanceDto balance = leaveService.getUserLeaveBalance(userId, year);
        
        return ResponseEntity.ok(balance);
    }

    /**
     * Demandes de congé d'un utilisateur (Manager/RH)
     */
    @GetMapping("/user/{userId}/requests")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Demandes de congé d'un utilisateur", 
               description = "Obtient les demandes de congé d'un utilisateur spécifique")
    public ResponseEntity<Page<LeaveRequestDto>> getUserLeaveRequests(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "submittedDate", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        
        Long requesterId = extractUserIdFromToken(httpRequest);
        
        Page<LeaveRequestDto> requests = leaveService.getUserLeaveRequests(userId, requesterId, pageable);
        
        return ResponseEntity.ok(requests);
    }

    /**
     * Solde de congés d'un utilisateur (Manager/RH)
     */
    @GetMapping("/user/{userId}/balance")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Solde de congés d'un utilisateur", 
               description = "Obtient le solde de congés d'un utilisateur spécifique")
    public ResponseEntity<LeaveBalanceDto> getUserLeaveBalance(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer year) {
        
        LeaveBalanceDto balance = leaveService.getUserLeaveBalance(userId, year);
        
        return ResponseEntity.ok(balance);
    }

    /**
     * Demandes en attente d'approbation manager
     */
    @GetMapping("/pending/manager")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Demandes en attente d'approbation manager", 
               description = "Obtient les demandes en attente d'approbation par le manager connecté")
    public ResponseEntity<List<LeaveRequestDto>> getPendingManagerApprovals(HttpServletRequest httpRequest) {
        Long managerId = extractUserIdFromToken(httpRequest);
        
        List<LeaveRequestDto> pendingRequests = leaveService.getPendingManagerApprovals(managerId);
        
        return ResponseEntity.ok(pendingRequests);
    }

    /**
     * Demandes en attente d'approbation RH
     */
    @GetMapping("/pending/hr")
    @PreAuthorize("hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Demandes en attente d'approbation RH", 
               description = "Obtient toutes les demandes en attente d'approbation par les RH")
    public ResponseEntity<List<LeaveRequestDto>> getPendingHrApprovals() {
        List<LeaveRequestDto> pendingRequests = leaveService.getPendingHrApprovals();
        
        return ResponseEntity.ok(pendingRequests);
    }

    /**
     * Demandes en attente d'approbation Directeur
     */
    @GetMapping("/pending/director")
    @PreAuthorize("hasRole('DIRECTOR')")
    @Operation(summary = "Demandes en attente d'approbation Directeur", 
               description = "Obtient toutes les demandes en attente d'approbation par le Directeur")
    public ResponseEntity<List<LeaveRequestDto>> getPendingDirectorApprovals() {
        List<LeaveRequestDto> pendingRequests = leaveService.getPendingDirectorApprovals();
        
        return ResponseEntity.ok(pendingRequests);
    }

    /**
     * Approbation par le manager
     */
    @PutMapping("/{leaveRequestId}/approve/manager")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Approbation manager", 
               description = "Approuve ou rejette une demande de congé en tant que manager")
    public ResponseEntity<LeaveRequestDto> approveByManager(
            @PathVariable Long leaveRequestId,
            @Valid @RequestBody ApproveLeaveRequest approvalDto,
            HttpServletRequest httpRequest) {
        
        Long managerId = extractUserIdFromToken(httpRequest);
        
        LeaveRequestDto approved = leaveService.approveByManager(leaveRequestId, approvalDto, managerId);
        
        return ResponseEntity.ok(approved);
    }

    /**
     * Approbation par les RH
     */
    @PutMapping("/{leaveRequestId}/approve/hr")
    @PreAuthorize("hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Approbation RH", 
               description = "Approuve ou rejette une demande de congé en tant que RH")
    public ResponseEntity<LeaveRequestDto> approveByHr(
            @PathVariable Long leaveRequestId,
            @Valid @RequestBody ApproveLeaveRequest approvalDto,
            HttpServletRequest httpRequest) {
        
        Long hrId = extractUserIdFromToken(httpRequest);
        
        LeaveRequestDto approved = leaveService.approveByHr(leaveRequestId, approvalDto, hrId);
        
        return ResponseEntity.ok(approved);
    }

    /**
     * Approbation par le Directeur
     */
    @PutMapping("/{leaveRequestId}/approve/director")
    @PreAuthorize("hasRole('DIRECTOR')")
    @Operation(summary = "Approbation Directeur", 
               description = "Approuve ou rejette une demande de congé en tant que Directeur")
    public ResponseEntity<LeaveRequestDto> approveByDirector(
            @PathVariable Long leaveRequestId,
            @Valid @RequestBody ApproveLeaveRequest approvalDto,
            HttpServletRequest httpRequest) {
        
        Long directorId = extractUserIdFromToken(httpRequest);
        
        LeaveRequestDto approved = leaveService.approveByDirector(leaveRequestId, approvalDto, directorId);
        
        return ResponseEntity.ok(approved);
    }

    /**
     * Annule une demande de congé
     */
    @PutMapping("/{leaveRequestId}/cancel")
    @Operation(summary = "Annuler une demande", 
               description = "Annule une demande de congé")
    public ResponseEntity<LeaveRequestDto> cancelLeaveRequest(
            @PathVariable Long leaveRequestId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserIdFromToken(httpRequest);
        String cancelReason = request.get("cancelReason");
        
        LeaveRequestDto cancelled = leaveService.cancelLeaveRequest(leaveRequestId, cancelReason, userId);
        
        return ResponseEntity.ok(cancelled);
    }

    /**
     * Détails d'une demande de congé
     */
    @GetMapping("/{leaveRequestId}")
    @Operation(summary = "Détails d'une demande", 
               description = "Obtient les détails d'une demande de congé")
    public ResponseEntity<LeaveRequestDto> getLeaveRequestDetails(@PathVariable Long leaveRequestId,
                                                                HttpServletRequest httpRequest) {
        // Cette méthode nécessiterait d'être ajoutée au service
        // Pour l'instant, rediriger vers les autres endpoints selon le contexte
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Utiliser les endpoints /my-requests ou /user/{userId}/requests");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok().build(); // À compléter avec la logique appropriée
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
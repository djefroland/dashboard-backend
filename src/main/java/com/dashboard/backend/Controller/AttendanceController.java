// src/main/java/com/dashboard/backend/Controller/AttendanceController.java
package com.dashboard.backend.Controller;

import com.dashboard.backend.dto.attendance.*;
import com.dashboard.backend.service.attendance.AttendanceService;
import com.dashboard.backend.service.auth.JwtService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Présences", description = "APIs pour la gestion des pointages et présences")
@SecurityRequirement(name = "Bearer Authentication")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final JwtService jwtService;

    /**
     * Pointage d'arrivée
     */
    @PostMapping("/clock-in")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('INTERN') or hasRole('TEAM_LEADER') or hasRole('HR')")
    @Operation(summary = "Pointer l'arrivée", 
               description = "Enregistre l'heure d'arrivée de l'utilisateur connecté")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Arrivée pointée avec succès"),
        @ApiResponse(responseCode = "400", description = "Déjà pointé ou données invalides"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Non autorisé à pointer")
    })
    public ResponseEntity<AttendanceDto> clockIn(@Valid @RequestBody ClockInRequest request,
                                                HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        
        AttendanceDto attendance = attendanceService.clockIn(userId, request, httpRequest);
        
        log.info("Pointage d'arrivée pour l'utilisateur ID: {} à {}", userId, LocalDateTime.now());
        
        return ResponseEntity.ok(attendance);
    }

    /**
     * Pointage de départ
     */
    @PostMapping("/clock-out")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('INTERN') or hasRole('TEAM_LEADER') or hasRole('HR')")
    @Operation(summary = "Pointer le départ", 
               description = "Enregistre l'heure de départ de l'utilisateur connecté")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Départ pointé avec succès"),
        @ApiResponse(responseCode = "400", description = "Pas de pointage d'arrivée ou déjà pointé"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<AttendanceDto> clockOut(@Valid @RequestBody ClockOutRequest request,
                                                 HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        
        AttendanceDto attendance = attendanceService.clockOut(userId, request);
        
        log.info("Pointage de départ pour l'utilisateur ID: {} à {}", userId, LocalDateTime.now());
        
        return ResponseEntity.ok(attendance);
    }

    /**
     * Début de pause
     */
    @PostMapping("/break/start")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('INTERN') or hasRole('TEAM_LEADER') or hasRole('HR')")
    @Operation(summary = "Commencer une pause", 
               description = "Démarre une pause pour l'utilisateur connecté")
    public ResponseEntity<AttendanceDto> startBreak(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        
        AttendanceDto attendance = attendanceService.startBreak(userId);
        
        return ResponseEntity.ok(attendance);
    }

    /**
     * Fin de pause
     */
    @PostMapping("/break/end")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('INTERN') or hasRole('TEAM_LEADER') or hasRole('HR')")
    @Operation(summary = "Terminer une pause", 
               description = "Termine la pause en cours pour l'utilisateur connecté")
    public ResponseEntity<AttendanceDto> endBreak(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        
        AttendanceDto attendance = attendanceService.endBreak(userId);
        
        return ResponseEntity.ok(attendance);
    }

    /**
     * Statut de présence actuel
     */
    @GetMapping("/status")
    @Operation(summary = "Statut de présence actuel", 
               description = "Obtient le statut de pointage actuel de l'utilisateur")
    public ResponseEntity<AttendanceDto> getCurrentStatus(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        
        AttendanceDto attendance = attendanceService.getCurrentAttendanceStatus(userId);
        
        return ResponseEntity.ok(attendance);
    }

    /**
     * Historique des présences de l'utilisateur connecté
     */
    @GetMapping("/my-history")
    @Operation(summary = "Mon historique de présences", 
               description = "Obtient l'historique des présences de l'utilisateur connecté")
    public ResponseEntity<Page<AttendanceDto>> getMyAttendanceHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserIdFromToken(httpRequest);
        
        Page<AttendanceDto> attendances = attendanceService.getUserAttendances(
            userId, startDate, endDate, userId, pageable);
        
        return ResponseEntity.ok(attendances);
    }

    /**
     * Mes statistiques de présence
     */
    @GetMapping("/my-stats")
    @Operation(summary = "Mes statistiques de présence", 
               description = "Obtient les statistiques de présence de l'utilisateur connecté")
    public ResponseEntity<AttendanceStatsDto> getMyAttendanceStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserIdFromToken(httpRequest);
        
        AttendanceStatsDto stats = attendanceService.getUserAttendanceStats(
            userId, startDate, endDate, userId);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Historique des présences d'un utilisateur (Manager/RH)
     */
    @GetMapping("/user/{userId}/history")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Historique de présences d'un utilisateur", 
               description = "Obtient l'historique des présences d'un utilisateur spécifique")
    public ResponseEntity<Page<AttendanceDto>> getUserAttendanceHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        
        Long requesterId = extractUserIdFromToken(httpRequest);
        
        Page<AttendanceDto> attendances = attendanceService.getUserAttendances(
            userId, startDate, endDate, requesterId, pageable);
        
        return ResponseEntity.ok(attendances);
    }

    /**
     * Statistiques de présence d'un utilisateur (Manager/RH)
     */
    @GetMapping("/user/{userId}/stats")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Statistiques de présence d'un utilisateur", 
               description = "Obtient les statistiques de présence d'un utilisateur spécifique")
    public ResponseEntity<AttendanceStatsDto> getUserAttendanceStats(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest httpRequest) {
        
        Long requesterId = extractUserIdFromToken(httpRequest);
        
        AttendanceStatsDto stats = attendanceService.getUserAttendanceStats(
            userId, startDate, endDate, requesterId);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Approbation de présences (Manager/RH)
     */
    @PutMapping("/approve")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Approuver des présences", 
               description = "Approuve une liste de présences")
    public ResponseEntity<Map<String, Object>> approveAttendances(
            @RequestBody List<Long> attendanceIds,
            HttpServletRequest httpRequest) {
        
        Long approverId = extractUserIdFromToken(httpRequest);
        
        attendanceService.approveAttendances(attendanceIds, approverId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Présences approuvées avec succès");
        response.put("approvedCount", attendanceIds.size());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Statistiques globales d'aujourd'hui (RH/Directeur)
     */
    @GetMapping("/today/stats")
    @PreAuthorize("hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Statistiques globales du jour", 
               description = "Obtient les statistiques de présence globales d'aujourd'hui")
    public ResponseEntity<Map<String, Object>> getTodayGlobalStats() {
        Map<String, Object> stats = attendanceService.getTodayGlobalStats();
        stats.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(stats);
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
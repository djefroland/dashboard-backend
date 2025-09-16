// src/main/java/com/dashboard/backend/Controller/ReportController.java
package com.dashboard.backend.Controller;

import com.dashboard.backend.dto.report.*;
import com.dashboard.backend.entity.report.Report;
import com.dashboard.backend.service.auth.JwtService;
import com.dashboard.backend.service.report.ReportService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Rapports", description = "APIs pour la génération et gestion des rapports")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final ReportService reportService;
    private final JwtService jwtService;

    /**
     * Crée une demande de rapport
     */
    @PostMapping
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Créer un rapport", description = "Lance la génération d'un nouveau rapport")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rapport créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<ReportDto> createReport(@Valid @RequestBody CreateReportRequest request,
            HttpServletRequest httpRequest) {
        Long authorId = extractUserIdFromToken(httpRequest);

        ReportDto report = reportService.createReport(request, authorId);

        log.info("Rapport créé: {} par l'utilisateur ID: {}", report.getTitle(), authorId);

        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    /**
     * Obtient un rapport par son ID
     */
    @GetMapping("/{reportId}")
    @Operation(summary = "Détails d'un rapport", description = "Obtient les détails d'un rapport par son ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rapport trouvé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Rapport non trouvé")
    })
    public ResponseEntity<ReportDto> getReport(@PathVariable Long reportId,
            HttpServletRequest httpRequest) {
        Long requesterId = extractUserIdFromToken(httpRequest);

        ReportDto report = reportService.getReportById(reportId, requesterId);

        return ResponseEntity.ok(report);
    }

    /**
     * Liste les rapports
     */
    @GetMapping
    @Operation(summary = "Lister les rapports", description = "Obtient la liste des rapports avec pagination et filtres")
    public ResponseEntity<Page<ReportDto>> getReports(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Report.ReportType reportType,
            @RequestParam(required = false) Report.ReportStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {

        Long requesterId = extractUserIdFromToken(httpRequest);
        Page<ReportDto> reports = reportService.getReports(search, reportType, status, requesterId, pageable);

        return ResponseEntity.ok(reports);
    }

    /**
     * Télécharge un rapport
     */
    @PostMapping("/{reportId}/download")
    @Operation(summary = "Télécharger un rapport", description = "Télécharge un rapport terminé")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Téléchargement initié"),
            @ApiResponse(responseCode = "400", description = "Rapport pas encore prêt"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Rapport non trouvé")
    })
    public ResponseEntity<Map<String, Object>> downloadReport(@PathVariable Long reportId,
            HttpServletRequest httpRequest) {
        Long requesterId = extractUserIdFromToken(httpRequest);

        reportService.downloadReport(reportId, requesterId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Téléchargement initié avec succès");
        response.put("reportId", reportId);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Mes rapports
     */
    @GetMapping("/my-reports")
    @Operation(summary = "Mes rapports", description = "Obtient la liste des rapports créés par l'utilisateur connecté")
    public ResponseEntity<Page<ReportDto>> getMyReports(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {

        Long userId = extractUserIdFromToken(httpRequest);

        Page<ReportDto> reports = reportService.getReports(
                null, null, null, userId, pageable);

        return ResponseEntity.ok(reports);
    }

    /**
     * Rapports publics
     */
    @GetMapping("/public")
    @Operation(summary = "Rapports publics", description = "Obtient la liste des rapports publics")
    public ResponseEntity<Page<ReportDto>> getPublicReports(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ReportDto> reports = reportService.getPublicReports(pageable);

        return ResponseEntity.ok(reports);
    }

    /**
     * Supprime un rapport
     */
    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Supprimer un rapport", description = "Supprime un rapport (RH/Directeur uniquement)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rapport supprimé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Rapport non trouvé")
    })
    public ResponseEntity<Map<String, Object>> deleteReport(@PathVariable Long reportId,
            HttpServletRequest httpRequest) {
        Long requesterId = extractUserIdFromToken(httpRequest);

        reportService.deleteReport(reportId, requesterId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Rapport supprimé avec succès");
        response.put("reportId", reportId);
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
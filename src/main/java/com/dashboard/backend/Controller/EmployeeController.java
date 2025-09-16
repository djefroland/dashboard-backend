package com.dashboard.backend.Controller;

import com.dashboard.backend.dto.employee.*;
import com.dashboard.backend.entity.employee.Employee;
import com.dashboard.backend.service.auth.JwtService;
import com.dashboard.backend.service.employee.EmployeeService;
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
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Employés", description = "APIs pour la gestion des employés et stagiaires")
@SecurityRequirement(name = "Bearer Authentication")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final JwtService jwtService;

    /**
     * Crée un nouvel employé
     */
    @PostMapping
    @PreAuthorize("hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Créer un employé", 
               description = "Crée un nouvel employé ou stagiaire")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Employé créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - RH requis")
    })
    public ResponseEntity<EmployeeDto> createEmployee(@Valid @RequestBody CreateEmployeeRequest request,
                                                     HttpServletRequest httpRequest) {
        Long createdById = extractUserIdFromToken(httpRequest);
        
        EmployeeDto employee = employeeService.createEmployee(request, createdById);
        
        log.info("Employé créé: {} par l'utilisateur ID: {}", employee.getEmployeeId(), createdById);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(employee);
    }

    /**
     * Met à jour un employé
     */
    @PutMapping("/{employeeId}")
    @PreAuthorize("hasRole('HR') or hasRole('DIRECTOR') or hasRole('TEAM_LEADER')")
    @Operation(summary = "Modifier un employé", 
               description = "Met à jour les informations d'un employé")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employé mis à jour"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Employé non trouvé")
    })
    public ResponseEntity<EmployeeDto> updateEmployee(@PathVariable Long employeeId,
                                                     @Valid @RequestBody UpdateEmployeeRequest request,
                                                     HttpServletRequest httpRequest) {
        Long updatedById = extractUserIdFromToken(httpRequest);
        
        EmployeeDto employee = employeeService.updateEmployee(employeeId, request, updatedById);
        
        return ResponseEntity.ok(employee);
    }

    /**
     * Obtient un employé par son ID
     */
    @GetMapping("/{employeeId}")
    @Operation(summary = "Détails d'un employé", 
               description = "Obtient les détails d'un employé par son ID")
    public ResponseEntity<EmployeeDto> getEmployee(@PathVariable Long employeeId,
                                                  HttpServletRequest httpRequest) {
        Long requesterId = extractUserIdFromToken(httpRequest);
        
        EmployeeDto employee = employeeService.getEmployeeById(employeeId, requesterId);
        
        return ResponseEntity.ok(employee);
    }

    /**
     * Liste tous les employés avec pagination et recherche
     */
    @GetMapping
    @Operation(summary = "Lister les employés", 
               description = "Obtient la liste des employés avec pagination, recherche et filtres")
    public ResponseEntity<Page<EmployeeDto>> getEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Employee.ContractType contractType,
            @RequestParam(required = false) Employee.EmployeeStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        
        Long requesterId = extractUserIdFromToken(httpRequest);
        
        Page<EmployeeDto> employees = employeeService.getEmployees(
            search, departmentId, contractType, status, pageable, requesterId);
        
        return ResponseEntity.ok(employees);
    }

    /**
     * Obtient les employés d'un département spécifique
     */
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Employés par département", 
               description = "Obtient tous les employés d'un département")
    public ResponseEntity<Page<EmployeeDto>> getEmployeesByDepartment(
            @PathVariable Long departmentId,
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable,
            HttpServletRequest httpRequest) {
        
        Long requesterId = extractUserIdFromToken(httpRequest);
        
        Page<EmployeeDto> employees = employeeService.getEmployees(
            null, departmentId, null, null, pageable, requesterId);
        
        return ResponseEntity.ok(employees);
    }

    /**
     * Obtient les stagiaires actifs
     */
    @GetMapping("/interns")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Liste des stagiaires", 
               description = "Obtient tous les stagiaires actifs")
    public ResponseEntity<Page<EmployeeDto>> getActiveInterns(
            @PageableDefault(size = 20, sort = "internshipStartDate", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        
        Long requesterId = extractUserIdFromToken(httpRequest);
        
        Page<EmployeeDto> interns = employeeService.getEmployees(
            null, null, Employee.ContractType.STAGE, Employee.EmployeeStatus.ACTIVE, pageable, requesterId);
        
        return ResponseEntity.ok(interns);
    }

    /**
     * Désactive un employé (soft delete)
     */
    @DeleteMapping("/{employeeId}")
    @PreAuthorize("hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Désactiver un employé", 
               description = "Désactive un employé (suppression logique)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employé désactivé"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - RH requis"),
        @ApiResponse(responseCode = "404", description = "Employé non trouvé")
    })
    public ResponseEntity<Map<String, Object>> deactivateEmployee(@PathVariable Long employeeId,
                                                                HttpServletRequest httpRequest) {
        Long deactivatedById = extractUserIdFromToken(httpRequest);
        
        employeeService.deactivateEmployee(employeeId, deactivatedById);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Employé désactivé avec succès");
        response.put("employeeId", employeeId);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Statistiques des employés (RH/Directeur)
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Statistiques des employés", 
               description = "Obtient les statistiques globales des employés")
    public ResponseEntity<Map<String, Object>> getEmployeeStatistics() {
        Map<String, Object> statistics = employeeService.getEmployeeStatistics();
        statistics.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * Obtient mon profil employé
     */
    @GetMapping("/my-profile")
    @Operation(summary = "Mon profil employé", 
               description = "Obtient les détails de l'employé connecté")
    public ResponseEntity<EmployeeDto> getMyEmployeeProfile(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        
        // Trouver l'employé par userId et le retourner
        EmployeeDto employee = employeeService.getEmployeeByUserId(userId);
        
        return ResponseEntity.ok(employee);
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
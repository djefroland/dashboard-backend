// src/main/java/com/dashboard/backend/Controller/DepartmentController.java
package com.dashboard.backend.Controller;

import com.dashboard.backend.dto.employee.DepartmentDto;
import com.dashboard.backend.service.employee.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Départements", description = "APIs pour la gestion des départements")
@SecurityRequirement(name = "Bearer Authentication")
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * Obtient tous les départements
     */
    @GetMapping
    @Operation(summary = "Lister les départements", 
               description = "Obtient la liste de tous les départements actifs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des départements récupérée"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        List<DepartmentDto> departments = departmentService.getAllDepartments();
        
        return ResponseEntity.ok(departments);
    }

    /**
     * Obtient un département par son ID
     */
    @GetMapping("/{departmentId}")
    @Operation(summary = "Détails d'un département", 
               description = "Obtient les détails d'un département par son ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Département trouvé"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Département non trouvé")
    })
    public ResponseEntity<DepartmentDto> getDepartment(@PathVariable Long departmentId) {
        DepartmentDto department = departmentService.getDepartmentById(departmentId);
        
        return ResponseEntity.ok(department);
    }
}
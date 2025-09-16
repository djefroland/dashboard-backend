// src/main/java/com/dashboard/backend/service/report/ReportService.java
package com.dashboard.backend.service.report;

import com.dashboard.backend.dto.report.CreateReportRequest;
import com.dashboard.backend.dto.report.ReportDto;
import com.dashboard.backend.entity.employee.Employee;
import com.dashboard.backend.entity.report.Report;
import com.dashboard.backend.entity.user.User;
import com.dashboard.backend.exception.custom.ResourceNotFoundException;
import com.dashboard.backend.exception.custom.UnauthorizedActionException;
import com.dashboard.backend.repository.*;
import com.dashboard.backend.security.UserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Crée un nouveau rapport
     */
    public ReportDto createReport(CreateReportRequest request, Long authorId) {
        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("Auteur non trouvé"));

        // Vérifier les autorisations
        if (!canCreateReport(author, request)) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à créer ce type de rapport");
        }

        // Valider la période
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("La date de fin doit être postérieure à la date de début");
        }

        // Sérialiser les paramètres
        String parametersJson = null;
        if (request.getParameters() != null) {
            try {
                parametersJson = objectMapper.writeValueAsString(request.getParameters());
            } catch (JsonProcessingException e) {
                log.warn("Erreur lors de la sérialisation des paramètres: {}", e.getMessage());
            }
        }

        // Créer le rapport
        Report report = Report.builder()
                .title(request.getTitle())
                .reportType(request.getReportType())
                .description(request.getDescription())
                .authorId(authorId)
                .parameters(parametersJson)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .departmentId(request.getDepartmentId())
                .employeeId(request.getEmployeeId())
                .userId(request.getUserId())
                .reportFormat(request.getReportFormat())
                .accessLevel(request.getAccessLevel())
                .isPublic(request.getIsPublic())
                .status(Report.ReportStatus.PENDING)
                .expiryDate(LocalDate.now().plusDays(30)) // 30 jours par défaut
                .build();

        Report saved = reportRepository.save(report);

        // Déclencher la génération asynchrone du rapport
        generateReportAsync(saved.getId());

        log.info("Rapport créé: {} par {} (ID: {})", request.getTitle(), author.getFullName(), authorId);

        return mapToReportDto(saved);
    }

    /**
     * Génère le rapport de manière asynchrone
     */
    public void generateReportAsync(Long reportId) {
        // Dans un environnement de production, utiliser @Async ou un système de queue
        try {
            Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));

            report.setStatus(Report.ReportStatus.GENERATING);
            reportRepository.save(report);

            // Générer les données selon le type de rapport
            Map<String, Object> reportData = generateReportData(report);

            // Sauvegarder les données
            try {
                String dataJson = objectMapper.writeValueAsString(reportData);
                report.setReportData(dataJson);
                report.setStatus(Report.ReportStatus.COMPLETED);
                report.setGenerationDate(LocalDate.now());
            } catch (JsonProcessingException e) {
                report.setStatus(Report.ReportStatus.FAILED);
                report.setErrorMessage("Erreur lors de la génération des données: " + e.getMessage());
                log.error("Erreur lors de la génération du rapport {}: {}", reportId, e.getMessage());
            }

            reportRepository.save(report);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport {}: {}", reportId, e.getMessage(), e);
            // Marquer le rapport comme échoué
            reportRepository.findById(reportId).ifPresent(r -> {
                r.setStatus(Report.ReportStatus.FAILED);
                r.setErrorMessage(e.getMessage());
                reportRepository.save(r);
            });
        }
    }

    /**
     * Génère les données du rapport selon son type
     */
    private Map<String, Object> generateReportData(Report report) {
        Map<String, Object> data = new HashMap<>();
        
        switch (report.getReportType()) {
            case ATTENDANCE:
                data = generateAttendanceReport(report);
                break;
            case LEAVE:
                data = generateLeaveReport(report);
                break;
            case EMPLOYEE:
                data = generateEmployeeReport(report);
                break;
            case ABSENCE:
                data = generateAbsenceReport(report);
                break;
            case OVERTIME:
                data = generateOvertimeReport(report);
                break;
            default:
                data.put("message", "Type de rapport non implémenté");
        }
        
        // Métadonnées communes
        data.put("reportId", report.getId());
        data.put("title", report.getTitle());
        data.put("generatedAt", LocalDate.now().toString());
        data.put("period", report.getStartDate() + " au " + report.getEndDate());
        data.put("author", userRepository.findById(report.getAuthorId()).map(User::getFullName).orElse("Inconnu"));
        
        return data;
    }

    /**
     * Génère un rapport de présence
     */
    private Map<String, Object> generateAttendanceReport(Report report) {
        Map<String, Object> data = new HashMap<>();
        
        if (report.getUserId() != null) {
            // Rapport individuel
            var attendances = attendanceRepository.findByUserIdAndDateBetween(
                report.getUserId(), report.getStartDate(), report.getEndDate());
            
            User user = userRepository.findById(report.getUserId()).orElse(null);
            Employee employee = employeeRepository.findByUserId(report.getUserId()).orElse(null);
            
            data.put("type", "individual");
            data.put("userName", user != null ? user.getFullName() : "Utilisateur inconnu");
            data.put("employeeId", employee != null ? employee.getEmployeeId() : null);
            data.put("attendances", attendances.size());
            data.put("totalHours", attendances.stream()
                .filter(a -> a.getHoursWorked() != null)
                .mapToDouble(a -> a.getHoursWorked().doubleValue())
                .sum());
            data.put("lateArrivals", attendances.stream().mapToInt(a -> a.isLate() ? 1 : 0).sum());
            data.put("overtimeHours", attendances.stream()
                .filter(a -> a.getOvertimeHours() != null)
                .mapToDouble(a -> a.getOvertimeHours().doubleValue())
                .sum());
        } else {
            // Rapport global ou par département
            data.put("type", "departmental");
            
            // Statistiques globales pour la période
            Object[] todayStats = attendanceRepository.getTodayStatistics();
            if (todayStats != null && todayStats.length >= 5) {
                data.put("totalAttendances", todayStats[0]);
                data.put("presentCount", todayStats[1]);
                data.put("absentCount", todayStats[2]);
                data.put("remoteCount", todayStats[3]);
                data.put("lateCount", todayStats[4]);
            }
        }
        
        return data;
    }

    /**
     * Génère un rapport de congés
     */
    private Map<String, Object> generateLeaveReport(Report report) {
        Map<String, Object> data = new HashMap<>();
        
        List<Object[]> leaveStats = leaveRequestRepository.getLeaveStatisticsByType(report.getStartDate().getYear());
        
        Map<String, Object> statsByType = new HashMap<>();
        double totalDays = 0;
        
        for (Object[] stat : leaveStats) {
            String leaveType = stat[0].toString();
            Long count = (Long) stat[1];
            Double days = ((Number) stat[2]).doubleValue();
            
            Map<String, Object> typeStat = new HashMap<>();
            typeStat.put("requests", count);
            typeStat.put("totalDays", days);
            
            statsByType.put(leaveType, typeStat);
            totalDays += days;
        }
        
        data.put("statsByType", statsByType);
        data.put("totalLeaveDays", totalDays);
        data.put("totalRequests", leaveStats.stream().mapToLong(s -> (Long) s[1]).sum());
        data.put("year", report.getStartDate().getYear());
        
        return data;
    }

    /**
     * Génère un rapport d'employés
     */
    private Map<String, Object> generateEmployeeReport(Report report) {
        Map<String, Object> data = new HashMap<>();
        
        List<Object[]> contractStats = employeeRepository.getEmployeeStatsByContractType();
        List<Object[]> deptStats = employeeRepository.getEmployeeStatsByDepartment();
        
        Map<String, Long> byContractType = new HashMap<>();
        for (Object[] stat : contractStats) {
            byContractType.put(stat[0].toString(), (Long) stat[1]);
        }
        
        Map<String, Long> byDepartment = new HashMap<>();
        for (Object[] stat : deptStats) {
            byDepartment.put((String) stat[0], (Long) stat[1]);
        }
        
        data.put("totalEmployees", employeeRepository.count());
        data.put("activeEmployees", employeeRepository.countByStatus(Employee.EmployeeStatus.ACTIVE));
        data.put("byContractType", byContractType);
        data.put("byDepartment", byDepartment);
        
        return data;
    }

    /**
     * Génère un rapport d'absences
     */
    private Map<String, Object> generateAbsenceReport(Report report) {
        Map<String, Object> data = new HashMap<>();
        
        // Rechercher les absences (statut ABSENT) dans la période
        var absences = attendanceRepository.findByStatusAndDateBetween(
            com.dashboard.backend.entity.attendance.AttendanceStatus.ABSENT, 
            report.getStartDate(), 
            report.getEndDate());
        
        data.put("totalAbsences", absences.size());
        data.put("period", report.getStartDate() + " au " + report.getEndDate());
        
        // Grouper par utilisateur
        Map<Long, Long> absencesByUser = absences.stream()
            .collect(Collectors.groupingBy(
                a -> a.getUserId(),
                Collectors.counting()
            ));
        
        data.put("absencesByUser", absencesByUser);
        
        return data;
    }

    /**
     * Génère un rapport d'heures supplémentaires
     */
    private Map<String, Object> generateOvertimeReport(Report report) {
        Map<String, Object> data = new HashMap<>();
        
        List<Object[]> overtimeStats = attendanceRepository.findTopOvertimeUsers(
            report.getStartDate(), report.getEndDate());
        
        Map<Long, Double> overtimeByUser = new HashMap<>();
        double totalOvertime = 0;
        
        for (Object[] stat : overtimeStats) {
            Long userId = (Long) stat[0];
            Double overtime = ((Number) stat[1]).doubleValue();
            overtimeByUser.put(userId, overtime);
            totalOvertime += overtime;
        }
        
        data.put("overtimeByUser", overtimeByUser);
        data.put("totalOvertimeHours", totalOvertime);
        data.put("averageOvertimePerUser", overtimeByUser.isEmpty() ? 0 : totalOvertime / overtimeByUser.size());
        
        return data;
    }

    /**
     * Obtient les rapports accessibles par un utilisateur
     */
    public Page<ReportDto> getAccessibleReports(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Page<Report> reports;
        
        if (user.getRole() == UserRole.DIRECTOR) {
            // Le directeur voit tous les rapports
            reports = reportRepository.findAll(pageable);
        } else if (user.getRole() == UserRole.HR) {
            // RH voit les rapports HR et publics
            reports = reportRepository.findByAccessLevelOrderByCreatedAtDesc(Report.AccessLevel.HR_ONLY, pageable);
        } else if (user.getRole().isManagerRole()) {
            // Managers voient les rapports management et publics
            reports = reportRepository.findByAccessLevelOrderByCreatedAtDesc(Report.AccessLevel.MANAGEMENT, pageable);
        } else {
            // Employés voient seulement leurs rapports et les publics
            reports = reportRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable);
        }

        return reports.map(this::mapToReportDto);
    }

    /**
     * Télécharge un rapport
     */
    public ReportDto downloadReport(Long reportId, Long userId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));

        if (!canAccessReport(userId, report)) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à télécharger ce rapport");
        }

        if (report.getStatus() != Report.ReportStatus.COMPLETED) {
            throw new ValidationException("Le rapport n'est pas encore prêt");
        }

        if (report.isExpired()) {
            throw new ValidationException("Le rapport a expiré");
        }

        // Incrémenter le compteur de téléchargements
        report.incrementDownloadCount();
        reportRepository.save(report);

        log.info("Rapport {} téléchargé par l'utilisateur {}", reportId, userId);

        return mapToReportDto(report);
    }

    /**
     * Supprime un rapport
     */
    public void deleteReport(Long reportId, Long userId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Seul l'auteur ou un administrateur peut supprimer
        if (!report.getAuthorId().equals(userId) && !user.getRole().canManageEmployees()) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à supprimer ce rapport");
        }

        reportRepository.delete(report);

        log.info("Rapport {} supprimé par {}", reportId, user.getFullName());
    }

    /**
     * Obtient un rapport par son ID
     */
    public ReportDto getReportById(Long reportId, Long requesterId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));

        if (!canAccessReport(requesterId, report)) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à accéder à ce rapport");
        }

        return mapToReportDto(report);
    }

    /**
     * Obtient les rapports avec filtres et pagination
     */
    public Page<ReportDto> getReports(String search, Report.ReportType reportType, Report.ReportStatus status, 
                                     Long requesterId, Pageable pageable) {
        User user = userRepository.findById(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        // Cas avec recherche textuelle
        if (search != null && !search.trim().isEmpty()) {
            Page<Report> searchResults = reportRepository.searchReports(search.trim(), pageable);
            return searchResults.map(this::mapToReportDto);
        }
        
        // Détermine quels rapports l'utilisateur peut voir
        Page<Report> accessibleReports;
        
        if (user.getRole() == UserRole.DIRECTOR) {
            accessibleReports = reportRepository.findAll(pageable);
        } else if (user.getRole() == UserRole.HR) {
            accessibleReports = reportRepository.findByAccessLevelOrderByCreatedAtDesc(Report.AccessLevel.HR_ONLY, pageable);
        } else if (user.getRole().isManagerRole()) {
            accessibleReports = reportRepository.findByAccessLevelOrderByCreatedAtDesc(Report.AccessLevel.MANAGEMENT, pageable);
        } else {
            accessibleReports = reportRepository.findByAuthorIdOrderByCreatedAtDesc(requesterId, pageable);
        }
        
        // Note: Idéalement, il faudrait implémenter des requêtes spécifiques avec filtres dans le Repository
        // Mais pour le moment, on retourne simplement les rapports accessibles
        
        return accessibleReports.map(this::mapToReportDto);
    }
    
    /**
     * Obtient les rapports publics
     */
    public Page<ReportDto> getPublicReports(Pageable pageable) {
        Page<Report> publicReports = reportRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable);
        return publicReports.map(this::mapToReportDto);
    }

    /**
     * Vérifie si un utilisateur peut créer un rapport
     */
    private boolean canCreateReport(User user, CreateReportRequest request) {
        // Directeur et RH peuvent créer tous types de rapports
        if (user.getRole().canManageEmployees()) {
            return true;
        }

        // Team Leaders peuvent créer des rapports sur leur équipe
        if (user.getRole() == UserRole.TEAM_LEADER) {
            return request.getReportType() != Report.ReportType.PAYROLL; // Pas de paie
        }

        // Employés peuvent créer des rapports individuels
        return request.getUserId() != null && request.getUserId().equals(user.getId());
    }

    /**
     * Vérifie si un utilisateur peut accéder à un rapport
     */
    private boolean canAccessReport(Long userId, Report report) {
        // Auteur peut toujours accéder
        if (report.getAuthorId().equals(userId)) {
            return true;
        }

        // Rapports publics
        if (report.getIsPublic()) {
            return true;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // Vérifier selon le niveau d'accès
        switch (report.getAccessLevel()) {
            case ALL:
                return true;
            case DIRECTOR_ONLY:
                return user.getRole() == UserRole.DIRECTOR;
            case HR_ONLY:
                return user.getRole() == UserRole.HR || user.getRole() == UserRole.DIRECTOR;
            case MANAGEMENT:
                return user.getRole().isManagerRole();
            case DEPARTMENT:
                // Vérifier si même département
                if (report.getDepartmentId() != null) {
                    Employee userEmployee = employeeRepository.findByUserId(userId).orElse(null);
                    return userEmployee != null && report.getDepartmentId().equals(userEmployee.getDepartmentId());
                }
                return false;
            case AUTHOR_ONLY:
            default:
                return false;
        }
    }

    /**
     * Mappe Report vers ReportDto
     */
    private ReportDto mapToReportDto(Report report) {
        User author = userRepository.findById(report.getAuthorId())
            .orElseThrow(() -> new ResourceNotFoundException("Auteur non trouvé"));

        String departmentName = null;
        if (report.getDepartmentId() != null) {
            departmentName = departmentRepository.findById(report.getDepartmentId())
                .map(d -> d.getName()).orElse("Département inconnu");
        }

        String employeeName = null;
        if (report.getEmployeeId() != null) {
            Employee employee = employeeRepository.findById(report.getEmployeeId()).orElse(null);
            if (employee != null) {
                User user = userRepository.findById(employee.getUserId()).orElse(null);
                employeeName = user != null ? user.getFullName() : "Employé inconnu";
            }
        }

        String userName = null;
        if (report.getUserId() != null) {
            userName = userRepository.findById(report.getUserId())
                .map(User::getFullName).orElse("Utilisateur inconnu");
        }

        // Taille de fichier formatée
        String fileSizeDisplay = "";
        if (report.getFileSize() != null) {
            long size = report.getFileSize();
            if (size < 1024) fileSizeDisplay = size + " B";
            else if (size < 1024 * 1024) fileSizeDisplay = String.format("%.1f KB", size / 1024.0);
            else fileSizeDisplay = String.format("%.1f MB", size / (1024.0 * 1024.0));
        }

        return ReportDto.builder()
                .id(report.getId())
                .title(report.getTitle())
                .reportType(report.getReportType())
                .reportTypeDisplayName(report.getReportType().getDisplayName())
                .description(report.getDescription())
                .authorId(report.getAuthorId())
                .authorName(author.getFullName())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .departmentId(report.getDepartmentId())
                .departmentName(departmentName)
                .employeeId(report.getEmployeeId())
                .employeeName(employeeName)
                .userId(report.getUserId())
                .userName(userName)
                .reportFormat(report.getReportFormat())
                .reportFormatDisplayName(report.getReportFormat().getDisplayName())
                .fileSize(report.getFileSize())
                .fileSizeDisplay(fileSizeDisplay)
                .status(report.getStatus())
                .statusDisplayName(report.getStatus().getDisplayName())
                .generationDate(report.getGenerationDate())
                .expiryDate(report.getExpiryDate())
                .isPublic(report.getIsPublic())
                .accessLevel(report.getAccessLevel())
                .accessLevelDisplayName(report.getAccessLevel().getDisplayName())
                .downloadCount(report.getDownloadCount())
                .errorMessage(report.getErrorMessage())
                .createdAt(report.getCreatedAt())
                .canDownload(report.getStatus() == Report.ReportStatus.COMPLETED && !report.isExpired())
                .canDelete(true) // À affiner selon les permissions
                .isExpired(report.isExpired())
                .downloadUrl(report.getStatus() == Report.ReportStatus.COMPLETED ? 
                    "/api/v1/reports/" + report.getId() + "/download" : null)
                .build();
    }
}
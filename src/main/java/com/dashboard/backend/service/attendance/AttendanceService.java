// src/main/java/com/dashboard/backend/service/attendance/AttendanceService.java
package com.dashboard.backend.service.attendance;

import com.dashboard.backend.dto.attendance.*;
import com.dashboard.backend.entity.attendance.Attendance;
import com.dashboard.backend.entity.attendance.AttendanceStatus;
import com.dashboard.backend.entity.employee.Employee;
import com.dashboard.backend.entity.user.User;
import com.dashboard.backend.exception.custom.ResourceNotFoundException;
import com.dashboard.backend.exception.custom.UnauthorizedActionException;
import com.dashboard.backend.repository.AttendanceRepository;
import com.dashboard.backend.repository.EmployeeRepository;
import com.dashboard.backend.repository.UserRepository;
import com.dashboard.backend.security.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageImpl;

import jakarta.validation.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Enregistre l'arrivée d'un utilisateur
     */
    public AttendanceDto clockIn(Long userId, ClockInRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Vérifier que l'utilisateur doit pointer
        if (!user.shouldTrackTime()) {
            throw new UnauthorizedActionException("Vous n'êtes pas tenu de pointer vos heures");
        }

        LocalDate today = LocalDate.now();
        
        // Vérifier s'il y a déjà un pointage pour aujourd'hui
        Optional<Attendance> existingAttendance = attendanceRepository.findByUserIdAndDate(userId, today);
        
        if (existingAttendance.isPresent() && existingAttendance.get().getClockIn() != null) {
            throw new ValidationException("Vous avez déjà pointé votre arrivée aujourd'hui");
        }

        Attendance attendance = existingAttendance.orElse(
            Attendance.builder()
                .userId(userId)
                .date(today)
                .build()
        );

        // Enregistrement de l'arrivée
        attendance.setClockIn(LocalDateTime.now());
        attendance.setLocation(request.getLocation());
        attendance.setStatus(request.getStatus() != null ? request.getStatus() : AttendanceStatus.PRESENT);
        attendance.setNotes(request.getNotes());
        attendance.setIsRemote(request.getIsRemote());
        attendance.setIpAddress(getClientIpAddress(httpRequest));
        attendance.setDeviceInfo(getUserAgent(httpRequest));

        // Déterminer automatiquement le statut selon l'heure
        if (attendance.isLate()) {
            attendance.setStatus(AttendanceStatus.LATE);
        }

        Attendance saved = attendanceRepository.save(attendance);

        log.info("Pointage d'arrivée enregistré pour {} (ID: {}) à {}", 
                user.getFullName(), userId, saved.getClockIn());

        return mapToAttendanceDto(saved);
    }

    /**
     * Enregistre le départ d'un utilisateur
     */
    public AttendanceDto clockOut(Long userId, ClockOutRequest request) {
        LocalDate today = LocalDate.now();
        
        Attendance attendance = attendanceRepository.findByUserIdAndDate(userId, today)
            .orElseThrow(() -> new ValidationException("Aucun pointage d'arrivée trouvé pour aujourd'hui"));

        if (attendance.getClockIn() == null) {
            throw new ValidationException("Vous devez d'abord pointer votre arrivée");
        }

        if (attendance.getClockOut() != null) {
            throw new ValidationException("Vous avez déjà pointé votre départ aujourd'hui");
        }

        // Enregistrement du départ
        attendance.setClockOut(LocalDateTime.now());
        if (request.getNotes() != null) {
            String existingNotes = attendance.getNotes();
            attendance.setNotes(existingNotes != null ? existingNotes + " | " + request.getNotes() : request.getNotes());
        }

        // Calcul automatique des heures
        attendance.calculateHours();

        // Vérifier si une approbation est nécessaire
        if (attendance.getOvertimeHours().compareTo(BigDecimal.valueOf(2)) > 0 || 
            attendance.getIsRemote() || 
            attendance.getStatus() == AttendanceStatus.HALF_DAY) {
            attendance.setApproved(false);
        } else {
            attendance.setApproved(true);
        }

        Attendance saved = attendanceRepository.save(attendance);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        log.info("Pointage de départ enregistré pour {} (ID: {}) à {} - {} heures travaillées", 
                user.getFullName(), userId, saved.getClockOut(), saved.getHoursWorked());

        return mapToAttendanceDto(saved);
    }

    /**
     * Démarre une pause
     */
    public AttendanceDto startBreak(Long userId) {
        LocalDate today = LocalDate.now();
        
        Attendance attendance = attendanceRepository.findByUserIdAndDate(userId, today)
            .orElseThrow(() -> new ValidationException("Aucun pointage trouvé pour aujourd'hui"));

        if (attendance.getClockIn() == null) {
            throw new ValidationException("Vous devez d'abord pointer votre arrivée");
        }

        if (attendance.getBreakStart() != null && attendance.getBreakEnd() == null) {
            throw new ValidationException("Une pause est déjà en cours");
        }

        attendance.setBreakStart(LocalDateTime.now());
        Attendance saved = attendanceRepository.save(attendance);

        log.info("Pause commencée pour l'utilisateur ID: {} à {}", userId, saved.getBreakStart());

        return mapToAttendanceDto(saved);
    }

    /**
     * Termine une pause
     */
    public AttendanceDto endBreak(Long userId) {
        LocalDate today = LocalDate.now();
        
        Attendance attendance = attendanceRepository.findByUserIdAndDate(userId, today)
            .orElseThrow(() -> new ValidationException("Aucun pointage trouvé pour aujourd'hui"));

        if (attendance.getBreakStart() == null) {
            throw new ValidationException("Aucune pause en cours");
        }

        if (attendance.getBreakEnd() != null) {
            throw new ValidationException("La pause est déjà terminée");
        }

        attendance.setBreakEnd(LocalDateTime.now());
        
        // Recalculer les heures si le départ est déjà pointé
        if (attendance.getClockOut() != null) {
            attendance.calculateHours();
        }

        Attendance saved = attendanceRepository.save(attendance);

        log.info("Pause terminée pour l'utilisateur ID: {} à {}", userId, saved.getBreakEnd());

        return mapToAttendanceDto(saved);
    }

    /**
     * Obtient les présences d'un utilisateur
     */
    public Page<AttendanceDto> getUserAttendances(Long userId, LocalDate startDate, LocalDate endDate, 
                                                 Long requesterId, Pageable pageable) {
        // Vérifier les autorisations
        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!canViewUserAttendance(requester, userId)) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à consulter ces présences");
        }

        Page<Attendance> attendances;
        
        if (startDate != null && endDate != null) {
            List<Attendance> attendanceList = attendanceRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
            attendances = new PageImpl<>(attendanceList, pageable, attendanceList.size());
        } else {
            attendances = attendanceRepository.findByUserIdOrderByDateDesc(userId, pageable);
        }

        return attendances.map(this::mapToAttendanceDto);
    }

    /**
     * Obtient le statut de présence actuel d'un utilisateur
     */
    public AttendanceDto getCurrentAttendanceStatus(Long userId) {
        LocalDate today = LocalDate.now();
        
        Optional<Attendance> attendance = attendanceRepository.findByUserIdAndDate(userId, today);
        
        if (attendance.isEmpty()) {
            // Créer un objet vide pour indiquer qu'aucun pointage n'a été fait
            return AttendanceDto.builder()
                .userId(userId)
                .date(today)
                .isComplete(false)
                .build();
        }

        return mapToAttendanceDto(attendance.get());
    }

    /**
     * Approuve des présences (Manager/RH)
     */
    public void approveAttendances(List<Long> attendanceIds, Long approverId) {
        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur approbateur non trouvé"));

        if (!approver.getRole().isManagerRole()) {
            throw new UnauthorizedActionException("Seuls les managers peuvent approuver les présences");
        }

        List<Attendance> attendances = attendanceRepository.findAllById(attendanceIds);
        
        for (Attendance attendance : attendances) {
            // Vérifier les autorisations spécifiques
            if (!canApproveAttendance(approver, attendance)) {
                continue;
            }

            attendance.setApproved(true);
            attendance.setApprovedById(approverId);
            attendance.setApprovalDate(LocalDateTime.now());
        }

        attendanceRepository.saveAll(attendances);

        log.info("{} présences approuvées par {} (ID: {})", 
                attendances.size(), approver.getFullName(), approverId);
    }

    /**
     * Obtient les statistiques de présence d'un utilisateur
     */
    public AttendanceStatsDto getUserAttendanceStats(Long userId, LocalDate startDate, LocalDate endDate, Long requesterId) {
        // Vérifier les autorisations
        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!canViewUserAttendance(requester, userId)) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à consulter ces statistiques");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Employee employee = employeeRepository.findByUserId(userId).orElse(null);

        // Calculs des statistiques
        Long totalDays = attendanceRepository.countAttendanceByUserAndPeriod(userId, startDate, endDate);
        Long presentDays = attendanceRepository.countByUserAndStatusAndPeriod(userId, AttendanceStatus.PRESENT, startDate, endDate);
        Long absentDays = attendanceRepository.countByUserAndStatusAndPeriod(userId, AttendanceStatus.ABSENT, startDate, endDate);
        Long lateDays = attendanceRepository.countByUserAndStatusAndPeriod(userId, AttendanceStatus.LATE, startDate, endDate);
        Long remoteDays = attendanceRepository.countByUserAndStatusAndPeriod(userId, AttendanceStatus.REMOTE, startDate, endDate);

        BigDecimal totalHours = attendanceRepository.sumHoursWorkedByUserAndPeriod(userId, startDate, endDate);
        BigDecimal totalOvertime = attendanceRepository.sumOvertimeByUserAndPeriod(userId, startDate, endDate);

        // Calculs des taux
        long workingDays = startDate.datesUntil(endDate.plusDays(1))
            .filter(date -> date.getDayOfWeek().getValue() <= 5) // Lundi à Vendredi
            .mapToInt(date -> 1)
            .sum();

        double attendanceRate = workingDays > 0 ? (presentDays.doubleValue() / workingDays) * 100 : 0;
        double punctualityRate = totalDays > 0 ? ((totalDays - lateDays) * 100.0 / totalDays) : 0;

        BigDecimal averageHours = totalDays > 0 ? 
            totalHours.divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return AttendanceStatsDto.builder()
                .userId(userId)
                .userName(user.getFullName())
                .employeeId(employee != null ? employee.getEmployeeId() : null)
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalDays(totalDays.intValue())
                .presentDays(presentDays.intValue())
                .absentDays(absentDays.intValue())
                .lateDays(lateDays.intValue())
                .remoteDays(remoteDays.intValue())
                .totalHoursWorked(totalHours)
                .averageHoursPerDay(averageHours)
                .totalOvertimeHours(totalOvertime)
                .attendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
                .punctualityRate(Math.round(punctualityRate * 100.0) / 100.0)
                .build();
    }

    /**
     * Obtient les statistiques globales d'aujourd'hui
     */
    public Map<String, Object> getTodayGlobalStats() {
        Object[] stats = attendanceRepository.getTodayStatistics();
        
        Map<String, Object> result = new HashMap<>();
        if (stats != null && stats.length >= 5) {
            result.put("total", stats[0]);
            result.put("present", stats[1]);
            result.put("absent", stats[2]);
            result.put("remote", stats[3]);
            result.put("late", stats[4]);
        }
        
        result.put("date", LocalDate.now());
        result.put("incompleteAttendances", attendanceRepository.findIncompleteAttendances(LocalDate.now()).size());
        
        return result;
    }

    /**
     * Vérifie si un utilisateur peut voir les présences d'un autre utilisateur
     */
    private boolean canViewUserAttendance(User requester, Long targetUserId) {
        // Peut voir ses propres présences
        if (requester.getId().equals(targetUserId)) {
            return true;
        }
        
        // RH et Directeur peuvent voir toutes les présences
        if (requester.getRole().canManageEmployees()) {
            return true;
        }
        
        // Team Leader peut voir les présences de son équipe
        if (requester.getRole() == UserRole.TEAM_LEADER) {
            Optional<Employee> targetEmployee = employeeRepository.findByUserId(targetUserId);
            if (targetEmployee.isPresent() && 
                requester.getId().equals(targetEmployee.get().getManagerId())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Vérifie si un utilisateur peut approuver une présence
     */
    private boolean canApproveAttendance(User approver, Attendance attendance) {
        // RH et Directeur peuvent approuver toutes les présences
        if (approver.getRole().canManageEmployees()) {
            return true;
        }
        
        // Team Leader peut approuver les présences de son équipe
        if (approver.getRole() == UserRole.TEAM_LEADER) {
            Optional<Employee> employee = employeeRepository.findByUserId(attendance.getUserId());
            return employee.isPresent() && 
                   approver.getId().equals(employee.get().getManagerId());
        }
        
        return false;
    }

    /**
     * Obtient l'adresse IP du client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Obtient le User-Agent
     */
    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
    
    /**
     * Convertit une entité Attendance en DTO
     */
    private AttendanceDto mapToAttendanceDto(Attendance attendance) {
        User user = userRepository.findById(attendance.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
            
        Employee employee = employeeRepository.findByUserId(attendance.getUserId()).orElse(null);
        
        String approverName = null;
        if (attendance.getApprovedById() != null) {
            approverName = userRepository.findById(attendance.getApprovedById())
                .map(User::getFullName)
                .orElse(null);
        }
        
        return AttendanceDto.builder()
            .id(attendance.getId())
            .userId(attendance.getUserId())
            .userName(user.getFullName())
            .employeeId(employee != null ? employee.getEmployeeId() : null)
            .date(attendance.getDate())
            .clockIn(attendance.getClockIn())
            .clockOut(attendance.getClockOut())
            .breakStart(attendance.getBreakStart())
            .breakEnd(attendance.getBreakEnd())
            .hoursWorked(attendance.getHoursWorked())
            .overtimeHours(attendance.getOvertimeHours())
            .status(attendance.getStatus())
            .statusDisplayName(attendance.getStatus() != null ? attendance.getStatus().getDisplayName() : null)
            .location(attendance.getLocation())
            .notes(attendance.getNotes())
            .isRemote(attendance.getIsRemote())
            .isLate(attendance.isLate())
            .isComplete(attendance.getClockIn() != null && attendance.getClockOut() != null)
            .isOnBreak(attendance.getBreakStart() != null && attendance.getBreakEnd() == null)
            .approved(attendance.getApproved())
            .approvedById(attendance.getApprovedById())
            .approvedByName(approverName)
            .approvalDate(attendance.getApprovalDate())
            .ipAddress(attendance.getIpAddress())
            .deviceInfo(attendance.getDeviceInfo())
            .build();
    }
}
// src/main/java/com/dashboard/backend/service/leave/LeaveService.java
package com.dashboard.backend.service.leave;

import com.dashboard.backend.dto.leave.*;
import com.dashboard.backend.entity.employee.Employee;
import com.dashboard.backend.entity.leave.LeaveRequest;
import com.dashboard.backend.entity.leave.LeaveType;
import com.dashboard.backend.entity.user.User;
import com.dashboard.backend.exception.custom.ResourceNotFoundException;
import com.dashboard.backend.exception.custom.UnauthorizedActionException;
import com.dashboard.backend.repository.EmployeeRepository;
import com.dashboard.backend.repository.LeaveRequestRepository;
import com.dashboard.backend.repository.UserRepository;
import com.dashboard.backend.security.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Crée une nouvelle demande de congé
     */
    public LeaveRequestDto createLeaveRequest(CreateLeaveRequest request, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Employee employee = employeeRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        // Validations
        validateLeaveRequest(request, userId);

        // Calculer le nombre de jours
        BigDecimal totalDays = calculateLeaveDays(request.getStartDate(), request.getEndDate());

        // Déterminer le workflow d'approbation
        boolean requiresManagerApproval = request.getLeaveType().requiresManagerApproval() && employee.getManagerId() != null;
        boolean requiresHrApproval = shouldRequireHrApproval(request, totalDays);
        boolean requiresDirectorApproval = shouldRequireDirectorApproval(request, totalDays);

        // Calculer la date de retour
        LocalDate returnDate = calculateReturnDate(request.getEndDate());

        // Créer la demande
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .userId(userId)
                .employeeId(employee.getEmployeeId())
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .returnDate(returnDate)
                .totalDays(totalDays)
                .reason(request.getReason())
                .emergencyContact(request.getEmergencyContact())
                .replacementPerson(request.getReplacementPerson())
                .handoverNotes(request.getHandoverNotes())
                .isUrgent(request.getIsUrgent())
                .requiresManagerApproval(requiresManagerApproval)
                .requiresHrApproval(requiresHrApproval)
                .requiresDirectorApproval(requiresDirectorApproval)
                .submittedDate(LocalDateTime.now())
                .build();

        // Initialiser les statuts d'approbation
        if (requiresManagerApproval) {
            leaveRequest.setManagerApprovalStatus(LeaveRequest.ApprovalStatus.PENDING);
        } else if (requiresHrApproval) {
            leaveRequest.setHrApprovalStatus(LeaveRequest.ApprovalStatus.PENDING);
        } else if (requiresDirectorApproval) {
            leaveRequest.setDirectorApprovalStatus(LeaveRequest.ApprovalStatus.PENDING);
        } else {
            // Cas rare : approbation automatique
            leaveRequest.setStatus(LeaveRequest.LeaveStatus.APPROVED);
            leaveRequest.setFinalApprovalDate(LocalDateTime.now());
        }

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        log.info("Demande de congé créée: {} jours de {} du {} au {} par {} (ID: {})",
                totalDays, request.getLeaveType().getDisplayName(),
                request.getStartDate(), request.getEndDate(),
                user.getFullName(), userId);

        return mapToLeaveRequestDto(saved);
    }

    /**
     * Approuve ou rejette une demande de congé (Manager)
     */
    public LeaveRequestDto approveByManager(Long leaveRequestId, ApproveLeaveRequest approvalDto, Long managerId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Demande de congé non trouvée"));

        User manager = userRepository.findById(managerId)
            .orElseThrow(() -> new ResourceNotFoundException("Manager non trouvé"));

        // Vérifier les autorisations
        if (!canApproveAsManager(manager, leaveRequest)) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à approuver cette demande");
        }

        if (leaveRequest.getManagerApprovalStatus() != LeaveRequest.ApprovalStatus.PENDING) {
            throw new ValidationException("Cette demande a déjà été traitée par un manager");
        }

        // Traiter l'approbation
        leaveRequest.setManagerApprovalStatus(approvalDto.getDecision());
        leaveRequest.setApprovedByManagerId(managerId);
        leaveRequest.setManagerApprovalDate(LocalDateTime.now());
        leaveRequest.setManagerComments(approvalDto.getComments());

        // Si escalade vers le directeur
        if (approvalDto.getEscalateToDirector() != null && approvalDto.getEscalateToDirector()) {
            leaveRequest.setRequiresDirectorApproval(true);
            leaveRequest.setDirectorApprovalStatus(LeaveRequest.ApprovalStatus.PENDING);
        }

        // Si approuvé et pas besoin d'autres approbations
        if (approvalDto.getDecision() == LeaveRequest.ApprovalStatus.APPROVED && 
            !leaveRequest.getRequiresHrApproval()) {
            leaveRequest.setStatus(LeaveRequest.LeaveStatus.APPROVED);
            leaveRequest.setFinalApprovalDate(LocalDateTime.now());
        } else if (approvalDto.getDecision() == LeaveRequest.ApprovalStatus.APPROVED && 
                   leaveRequest.getRequiresHrApproval()) {
            leaveRequest.setHrApprovalStatus(LeaveRequest.ApprovalStatus.PENDING);
        }

        // Mettre à jour le statut global
        leaveRequest.updateOverallStatus();

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);

        log.info("Demande de congé {} {} par le manager {} (ID: {})",
                leaveRequestId,
                approvalDto.getDecision() == LeaveRequest.ApprovalStatus.APPROVED ? "approuvée" : "rejetée",
                manager.getFullName(), managerId);

        return mapToLeaveRequestDto(updated);
    }

    /**
     * Approuve ou rejette une demande de congé (RH)
     */
    public LeaveRequestDto approveByHr(Long leaveRequestId, ApproveLeaveRequest approvalDto, Long hrId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Demande de congé non trouvée"));

        User hr = userRepository.findById(hrId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur RH non trouvé"));

        if (!hr.getRole().canApproveLeaves()) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à approuver les congés");
        }

        if (leaveRequest.getHrApprovalStatus() != LeaveRequest.ApprovalStatus.PENDING) {
            throw new ValidationException("Cette demande a déjà été traitée par les RH");
        }

        // Vérifier que l'approbation manager est faite (si nécessaire)
        if (leaveRequest.getRequiresManagerApproval() && 
            leaveRequest.getManagerApprovalStatus() != LeaveRequest.ApprovalStatus.APPROVED) {
            throw new ValidationException("La demande doit d'abord être approuvée par le manager");
        }

        // Traiter l'approbation RH
        leaveRequest.setHrApprovalStatus(approvalDto.getDecision());
        leaveRequest.setApprovedByHrId(hrId);
        leaveRequest.setHrApprovalDate(LocalDateTime.now());
        leaveRequest.setHrComments(approvalDto.getComments());

        // Si escalade vers le directeur
        if (approvalDto.getEscalateToDirector() != null && approvalDto.getEscalateToDirector()) {
            leaveRequest.setRequiresDirectorApproval(true);
            leaveRequest.setDirectorApprovalStatus(LeaveRequest.ApprovalStatus.PENDING);
        }

        // Mettre à jour le statut global
        leaveRequest.updateOverallStatus();

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);

        log.info("Demande de congé {} {} par les RH {} (ID: {})",
                leaveRequestId,
                approvalDto.getDecision() == LeaveRequest.ApprovalStatus.APPROVED ? "approuvée" : "rejetée",
                hr.getFullName(), hrId);

        return mapToLeaveRequestDto(updated);
    }

    /**
     * Approuve ou rejette une demande de congé (Directeur)
     */
    public LeaveRequestDto approveByDirector(Long leaveRequestId, ApproveLeaveRequest approvalDto, Long directorId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Demande de congé non trouvée"));

        User director = userRepository.findById(directorId)
            .orElseThrow(() -> new ResourceNotFoundException("Directeur non trouvé"));

        if (director.getRole() != UserRole.DIRECTOR) {
            throw new UnauthorizedActionException("Seul le Directeur peut effectuer cette approbation");
        }

        if (leaveRequest.getDirectorApprovalStatus() != LeaveRequest.ApprovalStatus.PENDING) {
            throw new ValidationException("Cette demande a déjà été traitée par le Directeur");
        }

        // Traiter l'approbation Directeur
        leaveRequest.setDirectorApprovalStatus(approvalDto.getDecision());
        leaveRequest.setApprovedByDirectorId(directorId);
        leaveRequest.setDirectorApprovalDate(LocalDateTime.now());
        leaveRequest.setDirectorComments(approvalDto.getComments());

        // Mettre à jour le statut global
        leaveRequest.updateOverallStatus();

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);

        log.info("Demande de congé {} {} par le Directeur {} (ID: {})",
                leaveRequestId,
                approvalDto.getDecision() == LeaveRequest.ApprovalStatus.APPROVED ? "approuvée" : "rejetée",
                director.getFullName(), directorId);

        return mapToLeaveRequestDto(updated);
    }

    /**
     * Annule une demande de congé
     */
    public LeaveRequestDto cancelLeaveRequest(Long leaveRequestId, String cancelReason, Long userId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Demande de congé non trouvée"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Vérifier les autorisations
        if (!leaveRequest.getUserId().equals(userId) && !user.getRole().canApproveLeaves()) {
            throw new UnauthorizedActionException("Vous ne pouvez pas annuler cette demande");
        }

        if (!leaveRequest.canBeCancelled()) {
            throw new ValidationException("Cette demande ne peut plus être annulée");
        }

        leaveRequest.setStatus(LeaveRequest.LeaveStatus.CANCELLED);
        leaveRequest.setCancelledDate(LocalDateTime.now());
        leaveRequest.setCancelReason(cancelReason);

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);

        log.info("Demande de congé {} annulée par {} (ID: {})",
                leaveRequestId, user.getFullName(), userId);

        return mapToLeaveRequestDto(updated);
    }

    /**
     * Obtient les demandes de congé d'un utilisateur
     */
    public Page<LeaveRequestDto> getUserLeaveRequests(Long userId, Long requesterId, Pageable pageable) {
        // Vérifier les autorisations
        if (!canViewUserLeaves(requesterId, userId)) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à consulter ces congés");
        }

        Page<LeaveRequest> leaveRequests = leaveRequestRepository.findByUserIdOrderBySubmittedDateDesc(userId, pageable);
        return leaveRequests.map(this::mapToLeaveRequestDto);
    }

    /**
     * Obtient les demandes en attente d'approbation pour un manager
     */
    public List<LeaveRequestDto> getPendingManagerApprovals(Long managerId) {
        User manager = userRepository.findById(managerId)
            .orElseThrow(() -> new ResourceNotFoundException("Manager non trouvé"));

        if (!manager.getRole().isManagerRole()) {
            throw new UnauthorizedActionException("Vous n'êtes pas un manager");
        }

        List<LeaveRequest> pendingRequests = leaveRequestRepository.findPendingManagerApprovals(managerId);
        return pendingRequests.stream()
                .map(this::mapToLeaveRequestDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtient les demandes en attente d'approbation RH
     */
    public List<LeaveRequestDto> getPendingHrApprovals() {
        List<LeaveRequest> pendingRequests = leaveRequestRepository.findPendingHrApprovals();
        return pendingRequests.stream()
                .map(this::mapToLeaveRequestDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtient les demandes en attente d'approbation Directeur
     */
    public List<LeaveRequestDto> getPendingDirectorApprovals() {
        List<LeaveRequest> pendingRequests = leaveRequestRepository.findPendingDirectorApprovals();
        return pendingRequests.stream()
                .map(this::mapToLeaveRequestDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtient le solde de congés d'un utilisateur
     */
    public LeaveBalanceDto getUserLeaveBalance(Long userId, Integer year) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Employee employee = employeeRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        if (year == null) {
            year = LocalDate.now().getYear();
        }

        // Calculer les congés pris
        BigDecimal annualLeaveTaken = leaveRequestRepository.sumApprovedLeaveByUserAndTypeAndYear(
            userId, LeaveType.ANNUAL_LEAVE, year);
        BigDecimal rttTaken = leaveRequestRepository.sumApprovedLeaveByUserAndTypeAndYear(
            userId, LeaveType.RTT, year);
        BigDecimal sickLeaveTaken = leaveRequestRepository.sumApprovedLeaveByUserAndTypeAndYear(
            userId, LeaveType.SICK_LEAVE, year);

        // Calculer les soldes
        BigDecimal annualLeaveRemaining = BigDecimal.valueOf(employee.getLeaveDaysEntitlement())
            .subtract(annualLeaveTaken);
        BigDecimal rttRemaining = BigDecimal.valueOf(LeaveType.RTT.getMaxDaysPerYear())
            .subtract(rttTaken);

        return LeaveBalanceDto.builder()
                .userId(userId)
                .userName(user.getFullName())
                .employeeId(employee.getEmployeeId())
                .annualLeaveEntitlement(employee.getLeaveDaysEntitlement())
                .annualLeaveTaken(annualLeaveTaken)
                .annualLeaveRemaining(annualLeaveRemaining)
                .rttEntitlement(LeaveType.RTT.getMaxDaysPerYear())
                .rttTaken(rttTaken)
                .rttRemaining(rttRemaining)
                .sickLeaveTaken(sickLeaveTaken)
                .totalLeaveTaken(annualLeaveTaken.add(rttTaken).add(sickLeaveTaken))
                .totalLeaveRemaining(annualLeaveRemaining.add(rttRemaining))
                .balanceAsOf(LocalDate.now())
                .year(year)
                .build();
    }

    /**
     * Valide une demande de congé
     */
    private void validateLeaveRequest(CreateLeaveRequest request, Long userId) {
        // Vérifier les dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("La date de fin doit être postérieure à la date de début");
        }

        if (request.getStartDate().isBefore(LocalDate.now().plusDays(1))) {
            throw new ValidationException("La demande doit être faite au moins 1 jour à l'avance");
        }

        // Vérifier les chevauchements
        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingLeaves(
            userId, request.getStartDate(), request.getEndDate());
        
        if (!overlapping.isEmpty()) {
            throw new ValidationException("Cette période chevauche avec une autre demande de congé");
        }

        // Vérifier le solde de congés
        if (request.getLeaveType() == LeaveType.ANNUAL_LEAVE || request.getLeaveType() == LeaveType.RTT) {
            LeaveBalanceDto balance = getUserLeaveBalance(userId, request.getStartDate().getYear());
            BigDecimal requestedDays = calculateLeaveDays(request.getStartDate(), request.getEndDate());
            
            if (request.getLeaveType() == LeaveType.ANNUAL_LEAVE && 
                balance.getAnnualLeaveRemaining().compareTo(requestedDays) < 0) {
                throw new ValidationException("Solde de congés payés insuffisant");
            }
            
            if (request.getLeaveType() == LeaveType.RTT && 
                balance.getRttRemaining().compareTo(requestedDays) < 0) {
                throw new ValidationException("Solde RTT insuffisant");
            }
        }
    }

    /**
     * Calcule le nombre de jours de congé
     */
    private BigDecimal calculateLeaveDays(LocalDate startDate, LocalDate endDate) {
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        // Exclure les week-ends
        long workingDays = startDate.datesUntil(endDate.plusDays(1))
            .filter(date -> date.getDayOfWeek().getValue() <= 5)
            .mapToLong(date -> 1)
            .sum();
            
        return BigDecimal.valueOf(workingDays);
    }

    /**
     * Calcule la date de retour au travail
     */
    private LocalDate calculateReturnDate(LocalDate endDate) {
        LocalDate returnDate = endDate.plusDays(1);
        
        // Si c'est un week-end, passer au lundi suivant
        while (returnDate.getDayOfWeek().getValue() > 5) {
            returnDate = returnDate.plusDays(1);
        }
        
        return returnDate;
    }

    /**
     * Détermine si une approbation RH est nécessaire
     */
    private boolean shouldRequireHrApproval(CreateLeaveRequest request, BigDecimal totalDays) {
        // Toujours nécessaire sauf cas spéciaux
        return request.getLeaveType() != LeaveType.SICK_LEAVE || totalDays.compareTo(BigDecimal.valueOf(3)) > 0;
    }

    /**
     * Détermine si une approbation Directeur est nécessaire
     */
    private boolean shouldRequireDirectorApproval(CreateLeaveRequest request, BigDecimal totalDays) {
        // Cas nécessitant l'approbation du directeur
        return totalDays.compareTo(BigDecimal.valueOf(15)) > 0 || // Plus de 15 jours
               request.getLeaveType() == LeaveType.UNPAID_LEAVE ||
               request.getLeaveType() == LeaveType.STUDY_LEAVE ||
               request.getIsUrgent();
    }

    /**
     * Vérifie si un manager peut approuver une demande
     */
    private boolean canApproveAsManager(User manager, LeaveRequest leaveRequest) {
        if (!manager.getRole().isManagerRole()) {
            return false;
        }

        // Le directeur peut approuver toutes les demandes
        if (manager.getRole() == UserRole.DIRECTOR) {
            return true;
        }

        // RH peut approuver toutes les demandes
        if (manager.getRole() == UserRole.HR) {
            return true;
        }

        // Team Leader peut approuver les demandes de son équipe
        if (manager.getRole() == UserRole.TEAM_LEADER) {
            Employee employee = employeeRepository.findByUserId(leaveRequest.getUserId()).orElse(null);
            return employee != null && manager.getId().equals(employee.getManagerId());
        }

        return false;
    }

    /**
     * Vérifie si un utilisateur peut voir les congés d'un autre
     */
    private boolean canViewUserLeaves(Long requesterId, Long targetUserId) {
        if (requesterId.equals(targetUserId)) {
            return true;
        }

        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // RH et Directeur peuvent voir tous les congés
        if (requester.getRole().canApproveLeaves()) {
            return true;
        }

        // Team Leader peut voir les congés de son équipe
        if (requester.getRole() == UserRole.TEAM_LEADER) {
            Employee targetEmployee = employeeRepository.findByUserId(targetUserId).orElse(null);
            return targetEmployee != null && requester.getId().equals(targetEmployee.getManagerId());
        }

        return false;
    }

    /**
     * Mappe LeaveRequest vers LeaveRequestDto
     */
    private LeaveRequestDto mapToLeaveRequestDto(LeaveRequest leaveRequest) {
        User user = userRepository.findById(leaveRequest.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Récupération des noms des approbateurs
        String managerName = null;
        if (leaveRequest.getApprovedByManagerId() != null) {
            managerName = userRepository.findById(leaveRequest.getApprovedByManagerId())
                .map(User::getFullName).orElse("Utilisateur inconnu");
        }

        String hrName = null;
        if (leaveRequest.getApprovedByHrId() != null) {
            hrName = userRepository.findById(leaveRequest.getApprovedByHrId())
                .map(User::getFullName).orElse("Utilisateur inconnu");
        }

        String directorName = null;
        if (leaveRequest.getApprovedByDirectorId() != null) {
            directorName = userRepository.findById(leaveRequest.getApprovedByDirectorId())
                .map(User::getFullName).orElse("Utilisateur inconnu");
        }

        // Déterminer le prochain approbateur
        String nextApprover = null;
        if (leaveRequest.getManagerApprovalStatus() == LeaveRequest.ApprovalStatus.PENDING) {
            nextApprover = "Manager";
        } else if (leaveRequest.getHrApprovalStatus() == LeaveRequest.ApprovalStatus.PENDING) {
            nextApprover = "RH";
        } else if (leaveRequest.getDirectorApprovalStatus() == LeaveRequest.ApprovalStatus.PENDING) {
            nextApprover = "Directeur";
        }

        // Calculer les jours jusqu'au début
        int daysUntilStart = (int) ChronoUnit.DAYS.between(LocalDate.now(), leaveRequest.getStartDate());

        return LeaveRequestDto.builder()
                .id(leaveRequest.getId())
                .userId(leaveRequest.getUserId())
                .userName(user.getFullName())
                .employeeId(leaveRequest.getEmployeeId())
                .leaveType(leaveRequest.getLeaveType())
                .leaveTypeDisplayName(leaveRequest.getLeaveType().getDisplayName())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .returnDate(leaveRequest.getReturnDate())
                .totalDays(leaveRequest.getTotalDays())
                .reason(leaveRequest.getReason())
                .emergencyContact(leaveRequest.getEmergencyContact())
                .replacementPerson(leaveRequest.getReplacementPerson())
                .handoverNotes(leaveRequest.getHandoverNotes())
                .status(leaveRequest.getStatus())
                .statusDisplayName(leaveRequest.getStatus().getDisplayName())
                .submittedDate(leaveRequest.getSubmittedDate())
                .finalApprovalDate(leaveRequest.getFinalApprovalDate())
                .managerApprovalStatus(leaveRequest.getManagerApprovalStatus())
                .managerApprovalStatusDisplay(leaveRequest.getManagerApprovalStatus() != null ? 
                    leaveRequest.getManagerApprovalStatus().getDisplayName() : null)
                .approvedByManagerId(leaveRequest.getApprovedByManagerId())
                .approvedByManagerName(managerName)
                .managerApprovalDate(leaveRequest.getManagerApprovalDate())
                .managerComments(leaveRequest.getManagerComments())
                .hrApprovalStatus(leaveRequest.getHrApprovalStatus())
                .hrApprovalStatusDisplay(leaveRequest.getHrApprovalStatus() != null ?
                    leaveRequest.getHrApprovalStatus().getDisplayName() : null)
                .approvedByHrId(leaveRequest.getApprovedByHrId())
                .approvedByHrName(hrName)
                .hrApprovalDate(leaveRequest.getHrApprovalDate())
                .hrComments(leaveRequest.getHrComments())
                .directorApprovalStatus(leaveRequest.getDirectorApprovalStatus())
                .directorApprovalStatusDisplay(leaveRequest.getDirectorApprovalStatus() != null ?
                    leaveRequest.getDirectorApprovalStatus().getDisplayName() : null)
                .approvedByDirectorId(leaveRequest.getApprovedByDirectorId())
                .approvedByDirectorName(directorName)
                .directorApprovalDate(leaveRequest.getDirectorApprovalDate())
                .directorComments(leaveRequest.getDirectorComments())
                .requiresManagerApproval(leaveRequest.getRequiresManagerApproval())
                .requiresHrApproval(leaveRequest.getRequiresHrApproval())
                .requiresDirectorApproval(leaveRequest.getRequiresDirectorApproval())
                .isUrgent(leaveRequest.getIsUrgent())
                .rejectionReason(leaveRequest.getRejectionReason())
                .cancelledDate(leaveRequest.getCancelledDate())
                .cancelReason(leaveRequest.getCancelReason())
                .canBeCancelled(leaveRequest.canBeCancelled())
                .isActive(leaveRequest.isActive())
                .isPending(leaveRequest.getStatus() == LeaveRequest.LeaveStatus.PENDING ||
                          leaveRequest.getStatus() == LeaveRequest.LeaveStatus.MANAGER_APPROVED ||
                          leaveRequest.getStatus() == LeaveRequest.LeaveStatus.HR_APPROVED)
                .nextApprover(nextApprover)
                .daysUntilStart(Math.max(0, daysUntilStart))
                .build();
    }
}
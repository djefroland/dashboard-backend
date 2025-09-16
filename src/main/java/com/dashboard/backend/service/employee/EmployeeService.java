// src/main/java/com/dashboard/backend/service/employee/EmployeeService.java
package com.dashboard.backend.service.employee;

import com.dashboard.backend.dto.employee.CreateEmployeeRequest;
import com.dashboard.backend.dto.employee.EmployeeDto;
import com.dashboard.backend.dto.employee.UpdateEmployeeRequest;
import com.dashboard.backend.entity.employee.Department;
import com.dashboard.backend.entity.employee.Employee;
import com.dashboard.backend.entity.employee.Position;
import com.dashboard.backend.entity.user.User;
import com.dashboard.backend.exception.custom.ResourceNotFoundException;
import com.dashboard.backend.exception.custom.UnauthorizedActionException;
import com.dashboard.backend.repository.*;
import com.dashboard.backend.security.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ValidationException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crée un nouvel employé
     */
    public EmployeeDto createEmployee(CreateEmployeeRequest request, Long createdById) {
        // Vérifier les autorisations
        User creator = userRepository.findById(createdById)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur créateur non trouvé"));

        if (!creator.getRole().canManageEmployees()) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à créer des employés");
        }

        // Vérifier l'unicité de l'ID employé
        if (employeeRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new ValidationException("Cet ID employé existe déjà");
        }

        User user;
        
        if (request.getUserId() != null) {
            // Utiliser un utilisateur existant
            user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
                
            // Vérifier qu'il n'est pas déjà employé
            if (employeeRepository.findByUserId(user.getId()).isPresent()) {
                throw new ValidationException("Cet utilisateur est déjà un employé");
            }
        } else {
            // Créer un nouvel utilisateur
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ValidationException("Ce nom d'utilisateur existe déjà");
            }
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ValidationException("Cette adresse email existe déjà");
            }

            user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode("TempPassword123!")) // Mot de passe temporaire
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(request.getRole() != null ? request.getRole() : UserRole.EMPLOYEE)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(false) // Forcer le changement de mot de passe
                .build();

            user = userRepository.save(user);
        }

        // Créer l'employé
        Employee employee = Employee.builder()
            .userId(user.getId())
            .employeeId(request.getEmployeeId())
            .hireDate(request.getHireDate())
            .contractType(request.getContractType())
            .jobTitle(request.getJobTitle())
            .departmentId(request.getDepartmentId())
            .positionId(request.getPositionId())
            .managerId(request.getManagerId())
            .address(request.getAddress())
            .city(request.getCity())
            .postalCode(request.getPostalCode())
            .country(request.getCountry() != null ? request.getCountry() : "France")
            .birthDate(request.getBirthDate())
            .nationality(request.getNationality())
            .emergencyContactName(request.getEmergencyContactName())
            .emergencyContactPhone(request.getEmergencyContactPhone())
            .salary(request.getSalary())
            .leaveDaysEntitlement(request.getLeaveDaysEntitlement() != null ? request.getLeaveDaysEntitlement() : 25)
            .internshipStartDate(request.getInternshipStartDate())
            .internshipEndDate(request.getInternshipEndDate())
            .schoolName(request.getSchoolName())
            .supervisorId(request.getSupervisorId())
            .internshipSubject(request.getInternshipSubject())
            .status(Employee.EmployeeStatus.ACTIVE)
            .active(true)
            .build();

        // Calculer les jours de congé restants
        employee.updateRemainingLeaveDays();

        Employee savedEmployee = employeeRepository.save(employee);

        log.info("Employé créé: {} {} (ID: {}) par {} (ID: {})",
                user.getFirstName(), user.getLastName(), savedEmployee.getEmployeeId(),
                creator.getFullName(), createdById);

        return mapToEmployeeDto(savedEmployee);
    }

    /**
     * Met à jour un employé
     */
    public EmployeeDto updateEmployee(Long employeeId, UpdateEmployeeRequest request, Long updatedById) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        // Vérifier les autorisations
        User updater = userRepository.findById(updatedById)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!canManageEmployee(updater, employee)) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à modifier cet employé");
        }

        // Mise à jour des informations
        employee.setJobTitle(request.getJobTitle());
        employee.setDepartmentId(request.getDepartmentId());
        employee.setPositionId(request.getPositionId());
        employee.setManagerId(request.getManagerId());
        employee.setAddress(request.getAddress());
        employee.setCity(request.getCity());
        employee.setPostalCode(request.getPostalCode());
        employee.setCountry(request.getCountry());
        employee.setBirthDate(request.getBirthDate());
        employee.setNationality(request.getNationality());
        employee.setEmergencyContactName(request.getEmergencyContactName());
        employee.setEmergencyContactPhone(request.getEmergencyContactPhone());
        employee.setSalary(request.getSalary());
        employee.setInternshipStartDate(request.getInternshipStartDate());
        employee.setInternshipEndDate(request.getInternshipEndDate());
        employee.setSchoolName(request.getSchoolName());
        employee.setSupervisorId(request.getSupervisorId());
        employee.setInternshipSubject(request.getInternshipSubject());
        employee.setStatus(request.getStatus());

        // Mise à jour des jours de congé si modifiés
        if (request.getLeaveDaysEntitlement() != null) {
            employee.setLeaveDaysEntitlement(request.getLeaveDaysEntitlement());
            employee.updateRemainingLeaveDays();
        }

        Employee updated = employeeRepository.save(employee);

        log.info("Employé {} mis à jour par {} (ID: {})",
                employee.getEmployeeId(), updater.getFullName(), updatedById);

        return mapToEmployeeDto(updated);
    }

    /**
     * Obtient un employé par son ID
     */
    public EmployeeDto getEmployeeById(Long employeeId, Long requesterId) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        // Vérifier les autorisations de lecture
        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!canViewEmployee(requester, employee)) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à consulter cet employé");
        }

        return mapToEmployeeDto(employee);
    }

    /**
     * Obtient tous les employés avec pagination et recherche
     */
    public Page<EmployeeDto> getEmployees(String search, Long departmentId, Employee.ContractType contractType, 
                                         Employee.EmployeeStatus status, Pageable pageable, Long requesterId) {
        // Vérifier les autorisations
        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Page<Employee> employees;

        if (search != null && !search.trim().isEmpty()) {
            employees = employeeRepository.searchEmployees(search.trim(), pageable);
        } else {
            // Appliquer les filtres selon le rôle
            if (requester.getRole().canManageEmployees()) {
                // RH/Directeur : voir tous les employés
                employees = employeeRepository.findAll(pageable);
            } else if (requester.getRole() == UserRole.TEAM_LEADER) {
                // Team Leader : voir son équipe
                employees = employeeRepository.findByManagerId(requesterId, pageable);
            } else {
                // Employé : voir seulement lui-même
                employees = Page.empty(pageable);
                Employee ownEmployee = employeeRepository.findByUserId(requesterId).orElse(null);
                if (ownEmployee != null) {
                    employees = new PageImpl<>(List.of(ownEmployee), pageable, 1);
                }
            }
        }

        return employees.map(this::mapToEmployeeDto);
    }

    /**
     * Désactive un employé (soft delete)
     */
    public void deactivateEmployee(Long employeeId, Long deactivatedById) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        User deactivator = userRepository.findById(deactivatedById)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!deactivator.getRole().canManageEmployees()) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à désactiver des employés");
        }

        employee.setActive(false);
        employee.setStatus(Employee.EmployeeStatus.TERMINATED);
        employee.setEndDate(LocalDate.now());

        // Désactiver aussi l'utilisateur associé
        User user = userRepository.findById(employee.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur associé non trouvé"));
        user.setEnabled(false);
        userRepository.save(user);

        employeeRepository.save(employee);

        log.info("Employé {} désactivé par {} (ID: {})",
                employee.getEmployeeId(), deactivator.getFullName(), deactivatedById);
    }

    /**
     * Obtient les statistiques des employés
     */
    public Map<String, Object> getEmployeeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalEmployees", employeeRepository.countByStatus(Employee.EmployeeStatus.ACTIVE));
        stats.put("totalInterns", employeeRepository.findActiveInterns().size());
        stats.put("onLeave", employeeRepository.countByStatus(Employee.EmployeeStatus.ON_LEAVE));
        stats.put("probation", employeeRepository.countByStatus(Employee.EmployeeStatus.PROBATION));
        
        // Statistiques par type de contrat
        List<Object[]> contractStats = employeeRepository.getEmployeeStatsByContractType();
        Map<String, Long> contractTypeStats = new HashMap<>();
        for (Object[] stat : contractStats) {
            contractTypeStats.put(stat[0].toString(), (Long) stat[1]);
        }
        stats.put("byContractType", contractTypeStats);
        
        // Statistiques par département
        List<Object[]> deptStats = employeeRepository.getEmployeeStatsByDepartment();
        Map<String, Long> departmentStats = new HashMap<>();
        for (Object[] stat : deptStats) {
            departmentStats.put((String) stat[0], (Long) stat[1]);
        }
        stats.put("byDepartment", departmentStats);
        
        return stats;
    }

    /**
     * Obtient un employé par son ID utilisateur
     */
    public EmployeeDto getEmployeeByUserId(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Profil employé non trouvé pour cet utilisateur"));
        
        return mapToEmployeeDto(employee);
    }

    /**
     * Vérifie si un utilisateur peut gérer un employé
     */
    private boolean canManageEmployee(User user, Employee employee) {
        // RH et Directeur peuvent gérer tous les employés
        if (user.getRole().canManageEmployees()) {
            return true;
        }
        
        // Team Leader peut gérer son équipe
        if (user.getRole() == UserRole.TEAM_LEADER && 
            employee.getManagerId() != null && employee.getManagerId().equals(user.getId())) {
            return true;
        }
        
        return false;
    }

    /**
     * Vérifie si un utilisateur peut voir un employé
     */
    private boolean canViewEmployee(User user, Employee employee) {
        // Peut gérer = peut voir
        if (canManageEmployee(user, employee)) {
            return true;
        }
        
        // Un employé peut voir ses propres informations
        if (employee.getUserId().equals(user.getId())) {
            return true;
        }
        
        return false;
    }

    /**
     * Mappe Employee vers EmployeeDto
     */
    private EmployeeDto mapToEmployeeDto(Employee employee) {
        // Récupération des informations utilisateur
        User user = userRepository.findById(employee.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Récupération des informations département
        String departmentName = null;
        if (employee.getDepartmentId() != null) {
            departmentName = departmentRepository.findById(employee.getDepartmentId())
                .map(Department::getName).orElse(null);
        }

        // Récupération des informations position
        String positionTitle = null;
        if (employee.getPositionId() != null) {
            positionTitle = positionRepository.findById(employee.getPositionId())
                .map(Position::getTitle).orElse(null);
        }

        // Récupération du nom du manager
        String managerName = null;
        if (employee.getManagerId() != null) {
            managerName = userRepository.findById(employee.getManagerId())
                .map(User::getFullName).orElse(null);
        }

        // Récupération du nom du superviseur (pour stagiaires)
        String supervisorName = null;
        if (employee.getSupervisorId() != null) {
            supervisorName = userRepository.findById(employee.getSupervisorId())
                .map(User::getFullName).orElse(null);
        }

        return EmployeeDto.builder()
                .id(employee.getId())
                .userId(employee.getUserId())
                .employeeId(employee.getEmployeeId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .hireDate(employee.getHireDate())
                .endDate(employee.getEndDate())
                .contractType(employee.getContractType())
                .contractTypeDisplayName(employee.getContractType().getDisplayName())
                .jobTitle(employee.getJobTitle())
                .departmentId(employee.getDepartmentId())
                .departmentName(departmentName)
                .positionId(employee.getPositionId())
                .positionTitle(positionTitle)
                .managerId(employee.getManagerId())
                .managerName(managerName)
                .address(employee.getAddress())
                .city(employee.getCity())
                .postalCode(employee.getPostalCode())
                .country(employee.getCountry())
                .birthDate(employee.getBirthDate())
                .nationality(employee.getNationality())
                .emergencyContactName(employee.getEmergencyContactName())
                .emergencyContactPhone(employee.getEmergencyContactPhone())
                .salary(employee.getSalary())
                .leaveDaysEntitlement(employee.getLeaveDaysEntitlement())
                .leaveDaysTaken(employee.getLeaveDaysTaken())
                .leaveDaysRemaining(employee.getLeaveDaysRemaining())
                .internshipStartDate(employee.getInternshipStartDate())
                .internshipEndDate(employee.getInternshipEndDate())
                .schoolName(employee.getSchoolName())
                .supervisorId(employee.getSupervisorId())
                .supervisorName(supervisorName)
                .internshipSubject(employee.getInternshipSubject())
                .status(employee.getStatus())
                .active(employee.getActive())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .isIntern(employee.isIntern())
                .canTakeLeave(employee.getLeaveDaysRemaining() > 0 && employee.getStatus() == Employee.EmployeeStatus.ACTIVE)
                .build();
    }
}

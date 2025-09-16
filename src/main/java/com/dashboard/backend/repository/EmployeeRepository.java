// src/main/java/com/dashboard/backend/repository/EmployeeRepository.java
package com.dashboard.backend.repository;

import com.dashboard.backend.entity.employee.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Trouve un employé par son ID utilisateur
     */
    Optional<Employee> findByUserId(Long userId);

    /**
     * Trouve un employé par son ID employé
     */
    Optional<Employee> findByEmployeeId(String employeeId);

    /**
     * Vérifie si un ID employé existe déjà
     */
    boolean existsByEmployeeId(String employeeId);

    /**
     * Trouve les employés par département
     */
    List<Employee> findByDepartmentId(Long departmentId);
    
    /**
     * Trouve les employés par département avec pagination
     */
    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);

    /**
     * Trouve les employés par manager
     */
    List<Employee> findByManagerId(Long managerId);
    
    /**
     * Trouve les employés par manager avec pagination
     */
    Page<Employee> findByManagerId(Long managerId, Pageable pageable);

    /**
     * Trouve les employés par type de contrat
     */
    List<Employee> findByContractType(Employee.ContractType contractType);

    /**
     * Trouve les stagiaires actifs
     */
    @Query("SELECT e FROM Employee e WHERE (e.contractType = 'STAGE' OR e.contractType = 'ALTERNANCE') AND e.active = true")
    List<Employee> findActiveInterns();

    /**
     * Trouve les employés actifs
     */
    List<Employee> findByActiveTrue();

    /**
     * Trouve les employés par statut
     */
    List<Employee> findByStatus(Employee.EmployeeStatus status);

    /**
     * Trouve les employés embauchés dans une période
     */
    List<Employee> findByHireDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Recherche d'employés par nom ou ID
     */
    @Query("SELECT e FROM Employee e JOIN User u ON e.userId = u.id WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.employeeId) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Employee> searchEmployees(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Compte les employés par département
     */
    long countByDepartmentId(Long departmentId);

    /**
     * Compte les employés par statut
     */
    long countByStatus(Employee.EmployeeStatus status);

    /**
     * Trouve les anniversaires du mois
     */
    @Query("SELECT e FROM Employee e WHERE MONTH(e.birthDate) = MONTH(CURRENT_DATE) AND e.active = true")
    List<Employee> findBirthdaysThisMonth();

    /**
     * Trouve les employés dont le contrat se termine bientôt
     */
    @Query("SELECT e FROM Employee e WHERE e.endDate BETWEEN CURRENT_DATE AND :futureDate AND e.active = true")
    List<Employee> findContractsEndingSoon(@Param("futureDate") LocalDate futureDate);

    /**
     * Statistiques des employés
     */
    @Query("SELECT e.contractType, COUNT(e) FROM Employee e WHERE e.active = true GROUP BY e.contractType")
    List<Object[]> getEmployeeStatsByContractType();

    @Query("SELECT d.name, COUNT(e) FROM Employee e JOIN Department d ON e.departmentId = d.id WHERE e.active = true GROUP BY d.name")
    List<Object[]> getEmployeeStatsByDepartment();
}
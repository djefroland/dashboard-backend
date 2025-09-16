// src/main/java/com/dashboard/backend/repository/DepartmentRepository.java
package com.dashboard.backend.repository;

import com.dashboard.backend.entity.employee.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * Trouve un département par son nom
     */
    Optional<Department> findByName(String name);

    /**
     * Vérifie si un nom de département existe déjà
     */
    boolean existsByName(String name);

    /**
     * Trouve les départements actifs
     */
    List<Department> findByActiveTrue();

    /**
     * Trouve les départements par manager
     */
    List<Department> findByManagerId(Long managerId);

    /**
     * Trouve les sous-départements
     */
    List<Department> findByParentDepartmentId(Long parentId);

    /**
     * Statistiques des départements
     */
    @Query("SELECT d, COUNT(e) FROM Department d LEFT JOIN Employee e ON d.id = e.departmentId WHERE d.active = true GROUP BY d")
    List<Object[]> getDepartmentStatistics();
}
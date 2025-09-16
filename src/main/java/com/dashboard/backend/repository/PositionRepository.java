// src/main/java/com/dashboard/backend/repository/PositionRepository.java
package com.dashboard.backend.repository;

import com.dashboard.backend.entity.employee.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    /**
     * Trouve les postes par département
     */
    List<Position> findByDepartmentId(Long departmentId);

    /**
     * Trouve les postes actifs
     */
    List<Position> findByActiveTrue();

    /**
     * Trouve les postes par niveau
     */
    List<Position> findByLevel(Position.PositionLevel level);

    /**
     * Vérifie si un titre de poste existe déjà
     */
    boolean existsByTitle(String title);
}
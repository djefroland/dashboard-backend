package com.dashboard.backend.repository;

import com.dashboard.backend.entity.user.User;
import com.dashboard.backend.security.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des utilisateurs
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Trouve un utilisateur par son nom d'utilisateur
     */
    Optional<User> findByUsername(String username);

    /**
     * Trouve un utilisateur par son email
     */
    Optional<User> findByEmail(String email);

    /**
     * Trouve un utilisateur par son nom d'utilisateur ou son email
     */
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);

    /**
     * Vérifie si un nom d'utilisateur existe déjà
     */
    boolean existsByUsername(String username);

    /**
     * Vérifie si un email existe déjà
     */
    boolean existsByEmail(String email);

    /**
     * Trouve tous les utilisateurs par rôle
     */
    List<User> findByRole(UserRole role);

    /**
     * Trouve tous les utilisateurs par rôle avec pagination
     */
    Page<User> findByRole(UserRole role, Pageable pageable);

    /**
     * Trouve tous les utilisateurs actifs
     */
    List<User> findByEnabledTrue();

    /**
     * Trouve tous les utilisateurs par département
     */
    List<User> findByDepartmentId(Long departmentId);

    /**
     * Trouve tous les utilisateurs sous un manager
     */
    List<User> findByManagerId(Long managerId);

    /**
     * Trouve les utilisateurs qui doivent pointer
     */
    @Query("SELECT u FROM User u WHERE u.requiresTimeTracking = true AND u.role != 'DIRECTOR'")
    List<User> findUsersWhoShouldTrackTime();

    /**
     * Trouve les utilisateurs par nom ou prénom (recherche partielle)
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Compte les utilisateurs par rôle
     */
    long countByRole(UserRole role);

    /**
     * Trouve les utilisateurs créés après une certaine date
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Trouve les utilisateurs qui ne se sont pas connectés depuis X jours
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginDate < :cutoffDate OR u.lastLoginDate IS NULL")
    List<User> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Trouve les Team Leaders avec leurs équipes
     */
    @Query("SELECT DISTINCT u FROM User u WHERE u.role = 'TEAM_LEADER' AND " +
           "EXISTS (SELECT 1 FROM User subordinate WHERE subordinate.managerId = u.id)")
    List<User> findTeamLeadersWithTeams();

    /**
     * Trouve les utilisateurs par rôles multiples
     */
    @Query("SELECT u FROM User u WHERE u.role IN :roles")
    List<User> findByRoleIn(@Param("roles") List<UserRole> roles);

    /**
     * Statistiques des utilisateurs actifs par rôle
     */
    @Query("SELECT u.role, COUNT(u) FROM User u WHERE u.enabled = true GROUP BY u.role")
    List<Object[]> getUserStatsByRole();
}
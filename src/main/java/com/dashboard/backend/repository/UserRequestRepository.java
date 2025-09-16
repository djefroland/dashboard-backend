// src/main/java/com/dashboard/backend/repository/UserRequestRepository.java
package com.dashboard.backend.repository;

import com.dashboard.backend.entity.user.UserRequest;
import com.dashboard.backend.security.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRequestRepository extends JpaRepository<UserRequest, Long> {

    /**
     * Trouve les demandes par statut
     */
    List<UserRequest> findByStatus(UserRequest.UserRequestStatus status);
    
    /**
     * Trouve les demandes par statut avec pagination
     */
    Page<UserRequest> findByStatus(UserRequest.UserRequestStatus status, Pageable pageable);

    /**
     * Trouve les demandes par type
     */
    List<UserRequest> findByRequestType(UserRequest.UserRequestType requestType);

    /**
     * Trouve les demandes créées par un utilisateur spécifique
     */
    List<UserRequest> findByRequestedById(Long requestedById);

    /**
     * Trouve les demandes en attente pour un rôle spécifique
     */
    @Query("SELECT ur FROM UserRequest ur WHERE ur.status = 'PENDING' AND ur.requestedRole = :role")
    List<UserRequest> findPendingRequestsByRole(@Param("role") UserRole role);

    /**
     * Trouve les demandes créées dans une période
     */
    List<UserRequest> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Compte les demandes en attente
     */
    long countByStatus(UserRequest.UserRequestStatus status);

    /**
     * Vérifie si un username est déjà demandé et en attente
     */
    boolean existsByUsernameAndStatus(String username, UserRequest.UserRequestStatus status);

    /**
     * Vérifie si un email est déjà demandé et en attente
     */
    boolean existsByEmailAndStatus(String email, UserRequest.UserRequestStatus status);
}
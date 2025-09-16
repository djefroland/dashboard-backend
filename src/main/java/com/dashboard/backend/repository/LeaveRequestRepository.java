// src/main/java/com/dashboard/backend/repository/LeaveRequestRepository.java
package com.dashboard.backend.repository;

import com.dashboard.backend.entity.leave.LeaveRequest;
import com.dashboard.backend.entity.leave.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    /**
     * Trouve les demandes de congé par utilisateur
     */
    List<LeaveRequest> findByUserIdOrderBySubmittedDateDesc(Long userId);
    
    Page<LeaveRequest> findByUserIdOrderBySubmittedDateDesc(Long userId, Pageable pageable);

    /**
     * Trouve les demandes par statut
     */
    List<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status);
    
    Page<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status, Pageable pageable);

    /**
     * Trouve les demandes en attente pour un manager spécifique
     */
    @Query("SELECT lr FROM LeaveRequest lr JOIN Employee e ON lr.userId = e.userId " +
           "WHERE e.managerId = :managerId AND lr.managerApprovalStatus = 'PENDING'")
    List<LeaveRequest> findPendingManagerApprovals(@Param("managerId") Long managerId);

    /**
     * Trouve les demandes en attente d'approbation RH
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.managerApprovalStatus = 'APPROVED' AND lr.hrApprovalStatus = 'PENDING'")
    List<LeaveRequest> findPendingHrApprovals();

    /**
     * Trouve les demandes en attente d'approbation directeur
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.hrApprovalStatus = 'APPROVED' AND lr.directorApprovalStatus = 'PENDING'")
    List<LeaveRequest> findPendingDirectorApprovals();

    /**
     * Trouve les congés actifs (en cours)
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'APPROVED' AND :currentDate BETWEEN lr.startDate AND lr.endDate")
    List<LeaveRequest> findActiveLeaves(@Param("currentDate") LocalDate currentDate);

    /**
     * Trouve les congés à venir dans une période
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'APPROVED' AND lr.startDate BETWEEN :startDate AND :endDate")
    List<LeaveRequest> findUpcomingLeaves(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Calcule les jours de congé pris par un utilisateur sur une année
     */
    @Query("SELECT COALESCE(SUM(lr.totalDays), 0) FROM LeaveRequest lr " +
           "WHERE lr.userId = :userId AND lr.leaveType = :leaveType AND lr.status = 'APPROVED' " +
           "AND YEAR(lr.startDate) = :year")
    BigDecimal sumApprovedLeaveByUserAndTypeAndYear(@Param("userId") Long userId, 
                                                   @Param("leaveType") LeaveType leaveType, 
                                                   @Param("year") Integer year);

    /**
     * Trouve les chevauchements de congés pour un utilisateur
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.userId = :userId AND lr.status IN ('APPROVED', 'PENDING') " +
           "AND ((lr.startDate BETWEEN :startDate AND :endDate) OR (lr.endDate BETWEEN :startDate AND :endDate) " +
           "OR (:startDate BETWEEN lr.startDate AND lr.endDate))")
    List<LeaveRequest> findOverlappingLeaves(@Param("userId") Long userId, 
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);

    /**
     * Trouve les demandes par département
     */
    @Query("SELECT lr FROM LeaveRequest lr JOIN Employee e ON lr.userId = e.userId " +
           "WHERE e.departmentId = :departmentId AND lr.status = :status")
    List<LeaveRequest> findByDepartmentAndStatus(@Param("departmentId") Long departmentId, 
                                               @Param("status") LeaveRequest.LeaveStatus status);

    /**
     * Statistiques des congés par type
     */
    @Query("SELECT lr.leaveType, COUNT(lr), SUM(lr.totalDays) FROM LeaveRequest lr " +
           "WHERE lr.status = 'APPROVED' AND YEAR(lr.startDate) = :year GROUP BY lr.leaveType")
    List<Object[]> getLeaveStatisticsByType(@Param("year") Integer year);

    /**
     * Trouve les demandes urgentes
     */
    List<LeaveRequest> findByIsUrgentTrueAndStatusIn(List<LeaveRequest.LeaveStatus> statuses);

    /**
     * Trouve les demandes soumises dans une période
     */
    List<LeaveRequest> findBySubmittedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Compte les demandes par statut
     */
    long countByStatus(LeaveRequest.LeaveStatus status);

    /**
     * Trouve les congés qui se terminent bientôt
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'APPROVED' AND lr.endDate BETWEEN CURRENT_DATE AND :futureDate")
    List<LeaveRequest> findLeavesEndingSoon(@Param("futureDate") LocalDate futureDate);
}
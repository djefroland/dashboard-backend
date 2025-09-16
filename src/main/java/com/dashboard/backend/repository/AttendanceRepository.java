// src/main/java/com/dashboard/backend/repository/AttendanceRepository.java
package com.dashboard.backend.repository;

import com.dashboard.backend.entity.attendance.Attendance;
import com.dashboard.backend.entity.attendance.AttendanceStatus;
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
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /**
     * Trouve la présence d'un utilisateur pour une date donnée
     */
    Optional<Attendance> findByUserIdAndDate(Long userId, LocalDate date);

    /**
     * Trouve les présences d'un utilisateur dans une période
     */
    List<Attendance> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Trouve les présences d'un utilisateur avec pagination
     */
    Page<Attendance> findByUserIdOrderByDateDesc(Long userId, Pageable pageable);

    /**
     * Trouve les présences par statut
     */
    List<Attendance> findByStatusAndDateBetween(AttendanceStatus status, LocalDate startDate, LocalDate endDate);

    /**
     * Trouve les présences non terminées (clockOut null)
     */
    @Query("SELECT a FROM Attendance a WHERE a.clockIn IS NOT NULL AND a.clockOut IS NULL AND a.date = :date")
    List<Attendance> findIncompleteAttendances(@Param("date") LocalDate date);

    /**
     * Trouve les présences d'aujourd'hui
     */
    @Query("SELECT a FROM Attendance a WHERE a.date = CURRENT_DATE ORDER BY a.clockIn ASC")
    List<Attendance> findTodayAttendances();

    /**
     * Trouve les retards du jour
     */
    @Query("SELECT a FROM Attendance a WHERE a.date = CURRENT_DATE AND TIME(a.clockIn) > '09:00:00'")
    List<Attendance> findTodayLateArrivals();

    /**
     * Statistiques de présence par utilisateur
     */
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.userId = :userId AND a.date BETWEEN :startDate AND :endDate")
    Long countAttendanceByUserAndPeriod(@Param("userId") Long userId, 
                                       @Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);

    /**
     * Compte les présences par statut pour un utilisateur
     */
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.userId = :userId AND a.status = :status AND a.date BETWEEN :startDate AND :endDate")
    Long countByUserAndStatusAndPeriod(@Param("userId") Long userId, 
                                      @Param("status") AttendanceStatus status,
                                      @Param("startDate") LocalDate startDate, 
                                      @Param("endDate") LocalDate endDate);

    /**
     * Somme des heures travaillées par utilisateur sur une période
     */
    @Query("SELECT COALESCE(SUM(a.hoursWorked), 0) FROM Attendance a WHERE a.userId = :userId AND a.date BETWEEN :startDate AND :endDate")
    BigDecimal sumHoursWorkedByUserAndPeriod(@Param("userId") Long userId, 
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);

    /**
     * Somme des heures supplémentaires par utilisateur
     */
    @Query("SELECT COALESCE(SUM(a.overtimeHours), 0) FROM Attendance a WHERE a.userId = :userId AND a.date BETWEEN :startDate AND :endDate")
    BigDecimal sumOvertimeByUserAndPeriod(@Param("userId") Long userId, 
                                        @Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);

    /**
     * Trouve les présences nécessitant une approbation
     */
    @Query("SELECT a FROM Attendance a WHERE a.approved = false AND (a.overtimeHours > 2 OR a.status IN ('REMOTE', 'HALF_DAY'))")
    List<Attendance> findPendingApprovals();

    /**
     * Présences par département (via Employee)
     */
    @Query("SELECT a FROM Attendance a JOIN Employee e ON a.userId = e.userId WHERE e.departmentId = :departmentId AND a.date BETWEEN :startDate AND :endDate")
    List<Attendance> findByDepartmentAndPeriod(@Param("departmentId") Long departmentId,
                                             @Param("startDate") LocalDate startDate, 
                                             @Param("endDate") LocalDate endDate);

    /**
     * Top des heures supplémentaires
     */
    @Query("SELECT a.userId, SUM(a.overtimeHours) as totalOvertime FROM Attendance a WHERE a.date BETWEEN :startDate AND :endDate GROUP BY a.userId ORDER BY totalOvertime DESC")
    List<Object[]> findTopOvertimeUsers(@Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);

    /**
     * Statistiques globales d'aujourd'hui
     */
    @Query("SELECT " +
           "COUNT(a) as total, " +
           "COUNT(CASE WHEN a.status = 'PRESENT' THEN 1 END) as present, " +
           "COUNT(CASE WHEN a.status = 'ABSENT' THEN 1 END) as absent, " +
           "COUNT(CASE WHEN a.status = 'REMOTE' THEN 1 END) as remote, " +
           "COUNT(CASE WHEN TIME(a.clockIn) > '09:00:00' THEN 1 END) as late " +
           "FROM Attendance a WHERE a.date = CURRENT_DATE")
    Object[] getTodayStatistics();
}
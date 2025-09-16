// src/main/java/com/dashboard/backend/repository/ReportRepository.java
package com.dashboard.backend.repository;

import com.dashboard.backend.entity.report.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Trouve les rapports par auteur
     */
    Page<Report> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    /**
     * Trouve les rapports par type
     */
    List<Report> findByReportType(Report.ReportType reportType);

    /**
     * Trouve les rapports par statut
     */
    List<Report> findByStatus(Report.ReportStatus status);

    /**
     * Trouve les rapports publics
     */
    Page<Report> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Trouve les rapports par niveau d'accès
     */
    List<Report> findByAccessLevel(Report.AccessLevel accessLevel);
    
    /**
     * Trouve les rapports par niveau d'accès avec pagination
     */
    Page<Report> findByAccessLevelOrderByCreatedAtDesc(Report.AccessLevel accessLevel, Pageable pageable);

    /**
     * Trouve les rapports expirés
     */
    @Query("SELECT r FROM Report r WHERE r.expiryDate IS NOT NULL AND r.expiryDate < CURRENT_DATE")
    List<Report> findExpiredReports();

    /**
     * Trouve les rapports par période
     */
    @Query("SELECT r FROM Report r WHERE r.generationDate BETWEEN :startDate AND :endDate")
    List<Report> findByGenerationDateBetween(@Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);

    /**
     * Trouve les rapports par département
     */
    Page<Report> findByDepartmentIdOrderByCreatedAtDesc(Long departmentId, Pageable pageable);

    /**
     * Recherche de rapports
     */
    @Query("SELECT r FROM Report r WHERE " +
           "LOWER(r.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Report> searchReports(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Statistiques des rapports
     */
    @Query("SELECT r.reportType, COUNT(r) FROM Report r GROUP BY r.reportType")
    List<Object[]> getReportStatsByType();

    @Query("SELECT r.status, COUNT(r) FROM Report r GROUP BY r.status")
    List<Object[]> getReportStatsByStatus();

    @Query("SELECT r.reportFormat, COUNT(r) FROM Report r GROUP BY r.reportFormat")
    List<Object[]> getReportStatsByFormat();

    /**
     * Rapports récents
     */
    @Query("SELECT r FROM Report r WHERE r.createdAt >= :date ORDER BY r.createdAt DESC")
    List<Report> findRecentReports(@Param("date") LocalDate date);

    /**
     * Compte les téléchargements totaux
     */
    @Query("SELECT COALESCE(SUM(r.downloadCount), 0) FROM Report r")
    Long getTotalDownloads();

    /**
     * Taille totale des fichiers
     */
    @Query("SELECT COALESCE(SUM(r.fileSize), 0) FROM Report r WHERE r.fileSize IS NOT NULL")
    Long getTotalFileSize();
}
// src/main/java/com/dashboard/backend/repository/NotificationRepository.java
package com.dashboard.backend.repository;

import com.dashboard.backend.entity.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Trouve les notifications par destinataire
     */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    /**
     * Trouve les notifications non lues
     */
    List<Notification> findByRecipientIdAndStatusOrderByCreatedAtDesc(Long recipientId, 
                                                                    Notification.NotificationStatus status);

    /**
     * Compte les notifications non lues
     */
    Long countByRecipientIdAndStatus(Long recipientId, Notification.NotificationStatus status);

    /**
     * Trouve les notifications par type
     */
    List<Notification> findByNotificationType(Notification.NotificationType notificationType);

    /**
     * Trouve les notifications par priorité
     */
    List<Notification> findByPriority(Notification.NotificationPriority priority);

    /**
     * Trouve les notifications expirées
     */
    @Query("SELECT n FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < CURRENT_TIMESTAMP")
    List<Notification> findExpiredNotifications();

    /**
     * Trouve les notifications programmées
     */
    @Query("SELECT n FROM Notification n WHERE n.scheduledFor IS NOT NULL AND n.scheduledFor <= CURRENT_TIMESTAMP AND n.status = 'UNREAD'")
    List<Notification> findScheduledNotifications();

    /**
     * Trouve les notifications par entité liée
     */
    List<Notification> findByRelatedEntityTypeAndRelatedEntityId(String entityType, Long entityId);

    /**
     * Trouve les notifications récentes
     */
    @Query("SELECT n FROM Notification n WHERE n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("since") LocalDateTime since);

    /**
     * Statistiques des notifications
     */
    @Query("SELECT n.notificationType, COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId GROUP BY n.notificationType")
    List<Object[]> getNotificationStatsByType(@Param("recipientId") Long recipientId);

    @Query("SELECT n.priority, COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId GROUP BY n.priority")
    List<Object[]> getNotificationStatsByPriority(@Param("recipientId") Long recipientId);

    /**
     * Nettoie les anciennes notifications
     */
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before AND n.status = 'ARCHIVED'")
    void deleteOldArchivedNotifications(@Param("before") LocalDateTime before);

    /**
     * Notifications par expéditeur
     */
    Page<Notification> findBySenderIdOrderByCreatedAtDesc(Long senderId, Pageable pageable);
}
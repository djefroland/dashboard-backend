// src/main/java/com/dashboard/backend/service/notification/NotificationService.java
package com.dashboard.backend.service.notification;

import com.dashboard.backend.dto.notification.*;
import com.dashboard.backend.entity.notification.Notification;
import com.dashboard.backend.entity.user.User;
import com.dashboard.backend.exception.custom.ResourceNotFoundException;
import com.dashboard.backend.exception.custom.UnauthorizedActionException;
import com.dashboard.backend.repository.NotificationRepository;
import com.dashboard.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Crée une notification
     */
    public NotificationDto createNotification(CreateNotificationRequest request) {
        User recipient = userRepository.findById(request.getRecipientId())
            .orElseThrow(() -> new ResourceNotFoundException("Destinataire non trouvé"));

        User sender = null;
        if (request.getSenderId() != null) {
            sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new ResourceNotFoundException("Expéditeur non trouvé"));
        }

        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .senderId(request.getSenderId())
                .title(request.getTitle())
                .message(request.getMessage())
                .notificationType(request.getNotificationType())
                .priority(request.getPriority())
                .actionUrl(request.getActionUrl())
                .actionLabel(request.getActionLabel())
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .metadata(request.getMetadata())
                .emailSent(request.getEmailSent())
                .pushSent(request.getPushSent())
                .scheduledFor(request.getScheduledFor())
                .expiresAt(request.getExpiresAt())
                .build();

        Notification saved = notificationRepository.save(notification);

        log.info("Notification créée pour {} (ID: {}) - {}", 
                recipient.getFullName(), request.getRecipientId(), request.getTitle());

        return mapToNotificationDto(saved);
    }

    /**
     * Méthode raccourcie pour créer une notification simple
     */
    public void createNotification(Long recipientId, String title, String message, String type) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .recipientId(recipientId)
                .title(title)
                .message(message)
                .notificationType(Notification.NotificationType.valueOf(type))
                .build();

        createNotification(request);
    }

    /**
     * Obtient les notifications d'un utilisateur
     */
    public Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository
            .findByRecipientIdOrderByCreatedAtDesc(userId, pageable);

        return notifications.map(this::mapToNotificationDto);
    }

    /**
     * Obtient les notifications non lues
     */
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository
            .findByRecipientIdAndStatusOrderByCreatedAtDesc(userId, Notification.NotificationStatus.UNREAD);

        return notifications.stream()
                .map(this::mapToNotificationDto)
                .toList();
    }

    /**
     * Marque une notification comme lue
     */
    public NotificationDto markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée"));

        if (!notification.getRecipientId().equals(userId)) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à modifier cette notification");
        }

        notification.markAsRead();
        Notification updated = notificationRepository.save(notification);

        return mapToNotificationDto(updated);
    }

    /**
     * Marque toutes les notifications comme lues
     */
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository
            .findByRecipientIdAndStatusOrderByCreatedAtDesc(userId, Notification.NotificationStatus.UNREAD);

        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
        }

        notificationRepository.saveAll(unreadNotifications);

        log.info("{} notifications marquées comme lues pour l'utilisateur {}", 
                unreadNotifications.size(), userId);
    }

    /**
     * Supprime une notification
     */
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée"));

        if (!notification.getRecipientId().equals(userId)) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à supprimer cette notification");
        }

        notificationRepository.delete(notification);

        log.info("Notification {} supprimée par l'utilisateur {}", notificationId, userId);
    }

    /**
     * Obtient les statistiques de notifications
     */
    public NotificationStatsDto getNotificationStats(Long userId) {
        Long totalNotifications = notificationRepository.countByRecipientIdAndStatus(
            userId, Notification.NotificationStatus.UNREAD) +
            notificationRepository.countByRecipientIdAndStatus(
            userId, Notification.NotificationStatus.READ);

        Long unreadNotifications = notificationRepository.countByRecipientIdAndStatus(
            userId, Notification.NotificationStatus.UNREAD);

        Long readNotifications = notificationRepository.countByRecipientIdAndStatus(
            userId, Notification.NotificationStatus.READ);

        Long archivedNotifications = notificationRepository.countByRecipientIdAndStatus(
            userId, Notification.NotificationStatus.ARCHIVED);

        // Statistiques par type et priorité
        List<Object[]> typeStats = notificationRepository.getNotificationStatsByType(userId);
        List<Object[]> priorityStats = notificationRepository.getNotificationStatsByPriority(userId);

        Map<String, Integer> notificationsByType = new HashMap<>();
        for (Object[] stat : typeStats) {
            notificationsByType.put(stat[0].toString(), ((Long) stat[1]).intValue());
        }

        Map<String, Integer> notificationsByPriority = new HashMap<>();
        for (Object[] stat : priorityStats) {
            notificationsByPriority.put(stat[0].toString(), ((Long) stat[1]).intValue());
        }

        // Notifications récentes
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);

        List<Notification> weeklyNotifications = notificationRepository.findRecentNotifications(weekAgo);
        List<Notification> monthlyNotifications = notificationRepository.findRecentNotifications(monthAgo);

        // Calcul du taux de lecture
        double readRate = totalNotifications > 0 ? 
            (readNotifications.doubleValue() / totalNotifications) * 100 : 0;

        return NotificationStatsDto.builder()
                .totalNotifications(totalNotifications.intValue())
                .unreadNotifications(unreadNotifications.intValue())
                .readNotifications(readNotifications.intValue())
                .archivedNotifications(archivedNotifications.intValue())
                .notificationsByType(notificationsByType)
                .notificationsByPriority(notificationsByPriority)
                .notificationsThisWeek(weeklyNotifications.size())
                .notificationsThisMonth(monthlyNotifications.size())
                .readRate(Math.round(readRate * 100.0) / 100.0)
                .build();
    }

    /**
     * Nettoie les anciennes notifications
     */
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(6);
        notificationRepository.deleteOldArchivedNotifications(cutoff);
        
        log.info("Nettoyage des notifications archivées antérieures au {}", cutoff);
    }

    /**
     * Notifie tous les utilisateurs RH
     */
    public void notifyHR(String title, String message) {
        List<User> hrUsers = userRepository.findByRole(com.dashboard.backend.security.UserRole.HR);
        
        for (User hrUser : hrUsers) {
            createNotification(hrUser.getId(), title, message, "SYSTEM");
        }
        
        log.info("Notification envoyée à {} utilisateurs RH: {}", hrUsers.size(), title);
    }

    /**
     * Calcule le temps écoulé depuis la création
     */
    private String calculateTimeAgo(LocalDateTime createdAt) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(createdAt, now);
        
        if (minutes < 1) {
            return "À l'instant";
        } else if (minutes < 60) {
            return "Il y a " + minutes + " minute" + (minutes > 1 ? "s" : "");
        } else if (minutes < 1440) { // 24 heures
            long hours = minutes / 60;
            return "Il y a " + hours + " heure" + (hours > 1 ? "s" : "");
        } else if (minutes < 43200) { // 30 jours
            long days = minutes / 1440;
            return "Il y a " + days + " jour" + (days > 1 ? "s" : "");
        } else {
            return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
    }

    /**
     * Mappe Notification vers NotificationDto
     */
    private NotificationDto mapToNotificationDto(Notification notification) {
        User recipient = userRepository.findById(notification.getRecipientId())
            .orElseThrow(() -> new ResourceNotFoundException("Destinataire non trouvé"));

        String senderName = null;
        if (notification.getSenderId() != null) {
            senderName = userRepository.findById(notification.getSenderId())
                .map(User::getFullName).orElse("Utilisateur inconnu");
        }

        return NotificationDto.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .recipientName(recipient.getFullName())
                .senderId(notification.getSenderId())
                .senderName(senderName)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .notificationType(notification.getNotificationType())
                .notificationTypeDisplayName(notification.getNotificationType().getDisplayName())
                .priority(notification.getPriority())
                .priorityDisplayName(notification.getPriority().getDisplayName())
                .status(notification.getStatus())
                .statusDisplayName(notification.getStatus().getDisplayName())
                .readAt(notification.getReadAt())
                .actionUrl(notification.getActionUrl())
                .actionLabel(notification.getActionLabel())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .metadata(notification.getMetadata())
                .emailSent(notification.getEmailSent())
                .pushSent(notification.getPushSent())
                .scheduledFor(notification.getScheduledFor())
                .expiresAt(notification.getExpiresAt())
                .createdAt(notification.getCreatedAt())
                .isRead(notification.getStatus() == Notification.NotificationStatus.READ)
                .isExpired(notification.isExpired())
                .isScheduled(notification.isScheduled())
                .timeAgo(calculateTimeAgo(notification.getCreatedAt()))
                .build();
    }
}
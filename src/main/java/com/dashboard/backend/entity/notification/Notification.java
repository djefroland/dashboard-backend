// src/main/java/com/dashboard/backend/entity/notification/Notification.java
package com.dashboard.backend.entity.notification;

import com.dashboard.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    @NotNull(message = "Le destinataire est obligatoire")
    private Long recipientId;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(nullable = false)
    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Le message est obligatoire")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type")
    @Builder.Default
    private NotificationType notificationType = NotificationType.INFO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "action_url")
    private String actionUrl;

    @Column(name = "action_label")
    private String actionLabel;

    // Métadonnées
    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    // Paramètres d'envoi
    @Builder.Default
    private Boolean emailSent = false;

    @Builder.Default
    private Boolean pushSent = false;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Types de notification
     */
    public enum NotificationType {
        INFO("Information"),
        SUCCESS("Succès"),
        WARNING("Attention"),
        ERROR("Erreur"),
        LEAVE_REQUEST("Demande de congé"),
        LEAVE_APPROVED("Congé approuvé"),
        LEAVE_REJECTED("Congé refusé"),
        ATTENDANCE_ALERT("Alerte présence"),
        OVERTIME_ALERT("Alerte heures supplémentaires"),
        REPORT_READY("Rapport prêt"),
        USER_REQUEST("Demande utilisateur"),
        SYSTEM("Système");

        private final String displayName;

        NotificationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Priorités de notification
     */
    public enum NotificationPriority {
        LOW("Faible"),
        NORMAL("Normale"),
        HIGH("Élevée"),
        URGENT("Urgente");

        private final String displayName;

        NotificationPriority(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Statuts de notification
     */
    public enum NotificationStatus {
        UNREAD("Non lue"),
        READ("Lue"),
        ARCHIVED("Archivée"),
        DELETED("Supprimée");

        private final String displayName;

        NotificationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Marque la notification comme lue
     */
    public void markAsRead() {
        this.status = NotificationStatus.READ;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Vérifie si la notification a expiré
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Vérifie si la notification est programmée
     */
    public boolean isScheduled() {
        return scheduledFor != null && LocalDateTime.now().isBefore(scheduledFor);
    }
}
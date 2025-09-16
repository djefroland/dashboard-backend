// src/main/java/com/dashboard/backend/dto/notification/NotificationDto.java
package com.dashboard.backend.dto.notification;

import com.dashboard.backend.entity.notification.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private Long id;
    private Long recipientId;
    private String recipientName;
    private Long senderId;
    private String senderName;
    
    private String title;
    private String message;
    
    private Notification.NotificationType notificationType;
    private String notificationTypeDisplayName;
    private Notification.NotificationPriority priority;
    private String priorityDisplayName;
    private Notification.NotificationStatus status;
    private String statusDisplayName;
    
    private LocalDateTime readAt;
    private String actionUrl;
    private String actionLabel;
    
    private String relatedEntityType;
    private Long relatedEntityId;
    private String metadata;
    
    private Boolean emailSent;
    private Boolean pushSent;
    private LocalDateTime scheduledFor;
    private LocalDateTime expiresAt;
    
    private LocalDateTime createdAt;
    
    // Propriétés calculées
    private Boolean isRead;
    private Boolean isExpired;
    private Boolean isScheduled;
    private String timeAgo; // "Il y a 2 heures"
}
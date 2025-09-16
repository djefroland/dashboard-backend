// src/main/java/com/dashboard/backend/dto/notification/CreateNotificationRequest.java
package com.dashboard.backend.dto.notification;

import com.dashboard.backend.entity.notification.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {
    
    @NotNull(message = "Le destinataire est obligatoire")
    private Long recipientId;
    
    private Long senderId;
    
    @NotBlank(message = "Le titre est obligatoire")
    private String title;
    
    @NotBlank(message = "Le message est obligatoire")
    private String message;
    
    @Builder.Default
    private Notification.NotificationType notificationType = Notification.NotificationType.INFO;
    
    @Builder.Default
    private Notification.NotificationPriority priority = Notification.NotificationPriority.NORMAL;
    
    private String actionUrl;
    private String actionLabel;
    
    private String relatedEntityType;
    private Long relatedEntityId;
    private String metadata;
    
    @Builder.Default
    private Boolean emailSent = false;
    
    @Builder.Default
    private Boolean pushSent = false;
    
    private LocalDateTime scheduledFor;
    private LocalDateTime expiresAt;
}
// src/main/java/com/dashboard/backend/dto/notification/NotificationStatsDto.java
package com.dashboard.backend.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationStatsDto {
    private Integer totalNotifications;
    private Integer unreadNotifications;
    private Integer readNotifications;
    private Integer archivedNotifications;
    
    private Map<String, Integer> notificationsByType;
    private Map<String, Integer> notificationsByPriority;
    
    private Integer notificationsThisWeek;
    private Integer notificationsThisMonth;
    
    private Double readRate; // Pourcentage de notifications lues
}
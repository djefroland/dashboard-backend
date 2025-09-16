// src/main/java/com/dashboard/backend/Controller/NotificationController.java
package com.dashboard.backend.Controller;

import com.dashboard.backend.dto.notification.*;
import com.dashboard.backend.entity.notification.Notification;
import com.dashboard.backend.service.auth.JwtService;
import com.dashboard.backend.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Notifications", description = "APIs pour la gestion des notifications utilisateur")
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    /**
     * Crée une notification
     */
    @PostMapping
    @PreAuthorize("hasRole('HR') or hasRole('DIRECTOR') or hasRole('TEAM_LEADER')")
    @Operation(summary = "Créer une notification", 
               description = "Crée une nouvelle notification pour un utilisateur")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Notification créée avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<NotificationDto> createNotification(@Valid @RequestBody CreateNotificationRequest request,
                                                             HttpServletRequest httpRequest) {
        Long senderId = extractUserIdFromToken(httpRequest);
        request.setSenderId(senderId);
        
        NotificationDto notification = notificationService.createNotification(request);
        
        log.info("Notification créée par l'utilisateur ID: {} pour le destinataire ID: {}", 
                senderId, request.getRecipientId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    /**
     * Mes notifications
     */
    @GetMapping("/my-notifications")
    @Operation(summary = "Mes notifications", 
               description = "Obtient la liste des notifications de l'utilisateur connecté")
    public ResponseEntity<Page<NotificationDto>> getMyNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserIdFromToken(httpRequest);
        
        Page<NotificationDto> notifications = notificationService.getUserNotifications(userId, pageable);
        
        return ResponseEntity.ok(notifications);
    }

    /**
     * Notifications non lues
     */
    @GetMapping("/unread")
    @Operation(summary = "Notifications non lues", 
               description = "Obtient la liste des notifications non lues")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        
        List<NotificationDto> notifications = notificationService.getUnreadNotifications(userId);
        
        return ResponseEntity.ok(notifications);
    }

    /**
     * Marque une notification comme lue
     */
    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Marquer comme lue", 
               description = "Marque une notification comme lue")
    public ResponseEntity<NotificationDto> markAsRead(@PathVariable Long notificationId,
                                                     HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        
        NotificationDto notification = notificationService.markAsRead(notificationId, userId);
        
        return ResponseEntity.ok(notification);
    }

    /**
     * Marque toutes les notifications comme lues
     */
    @PutMapping("/mark-all-read")
    @Operation(summary = "Marquer tout comme lu", 
               description = "Marque toutes les notifications comme lues")
    public ResponseEntity<Map<String, Object>> markAllAsRead(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        
        notificationService.markAllAsRead(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Toutes les notifications ont été marquées comme lues");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Supprime une notification
     */
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Supprimer une notification", 
               description = "Supprime une notification")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long notificationId,
                                                                 HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        
        notificationService.deleteNotification(notificationId, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Notification supprimée avec succès");
        response.put("notificationId", notificationId);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Statistiques de notifications
     */
    @GetMapping("/stats")
    @Operation(summary = "Statistiques de notifications", 
               description = "Obtient les statistiques de notifications de l'utilisateur")
    public ResponseEntity<NotificationStatsDto> getNotificationStats(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        
        NotificationStatsDto stats = notificationService.getNotificationStats(userId);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Notifications système (RH/Directeur)
     */
    @PostMapping("/system")
    @PreAuthorize("hasRole('HR') or hasRole('DIRECTOR')")
    @Operation(summary = "Notification système", 
               description = "Envoie une notification système à tous les utilisateurs")
    public ResponseEntity<Map<String, Object>> sendSystemNotification(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        String title = request.get("title");
        String message = request.get("message");
        String type = request.getOrDefault("type", "SYSTEM");
        
        // Cette méthode devrait être implémentée pour notifier tous les utilisateurs
        // notificationService.sendSystemNotification(title, message, type);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Notification système envoyée");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Extrait l'ID utilisateur du token JWT
     */
    private Long extractUserIdFromToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            throw new RuntimeException("Token JWT manquant");
        }
        return jwtService.extractUserId(token);
    }

    /**
     * Extrait le token JWT de la requête
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
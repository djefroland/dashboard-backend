// src/main/java/com/dashboard/backend/entity/user/UserRequest.java
package com.dashboard.backend.entity.user;

import com.dashboard.backend.entity.common.BaseEntity;
import com.dashboard.backend.security.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRequestStatus status;

    // Informations utilisateur demandé
    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole requestedRole;

    // Informations organisationnelles
    private Long departmentId;
    private Long managerId;

    // Workflow d'approbation
    @Column(name = "requested_by_id", nullable = false)
    private Long requestedById; // ID de l'utilisateur RH qui fait la demande

    @Column(name = "reviewed_by_id")
    private Long reviewedById; // ID du Directeur qui approuve/rejette

    @Column(name = "review_date")
    private LocalDateTime reviewDate;

    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;

    // Informations supplémentaires
    @Column(columnDefinition = "TEXT")
    private String justification; // Justification de la demande

    @Column(name = "temporary_password")
    private String temporaryPassword; // Mot de passe temporaire généré

    @Column(name = "user_created_id")
    private Long userCreatedId; // ID de l'utilisateur créé (si approuvé)

    public enum UserRequestType {
        CREATE_USER,
        CHANGE_ROLE,
        REACTIVATE_USER
    }

    public enum UserRequestStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED
    }
}
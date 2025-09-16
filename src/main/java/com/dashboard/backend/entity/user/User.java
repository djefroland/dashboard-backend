package com.dashboard.backend.entity.user;

import com.dashboard.backend.entity.common.BaseEntity;
import com.dashboard.backend.security.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * Entité User - Classe de base pour tous les utilisateurs du système
 * (Directeur, RH, Team Leader, Employé, Stagiaire)
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir entre 3 et 50 caractères")
    private String username;

    @Column(unique = true, nullable = false)
    @Email(message = "L'email doit être valide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 50, message = "Le prénom ne peut pas dépasser 50 caractères")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 50, message = "Le nom ne peut pas dépasser 50 caractères")
    private String lastName;

    @Size(max = 15, message = "Le téléphone ne peut pas dépasser 15 caractères")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    // Propriétés de statut du compte
    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private Boolean accountNonExpired = true;

    @Builder.Default
    private Boolean accountNonLocked = true;

    @Builder.Default
    private Boolean credentialsNonExpired = true;

    // Propriétés spécifiques selon le rôle
    @Builder.Default
    private Boolean requiresTimeTracking = true; // false pour DIRECTOR

    // Dates importantes
    private LocalDateTime lastLoginDate;
    private LocalDateTime passwordLastChanged;
    private LocalDateTime emailVerifiedAt;

    // Relation avec le département (pour TL, Employés, Stagiaires)
    private Long departmentId;

    // Relation avec le manager (pour hiérarchie)
    private Long managerId;

    /**
     * Détermine si l'utilisateur doit pointer ses heures
     */
    public boolean shouldTrackTime() {
        return this.role != UserRole.DIRECTOR && this.requiresTimeTracking;
    }

    /**
     * Vérifie si l'utilisateur a le rôle spécifié
     */
    public boolean hasRole(UserRole role) {
        return this.role == role;
    }

    /**
     * Vérifie si l'utilisateur est un manager (TL, RH, ou Directeur)
     */
    public boolean isManager() {
        return this.role == UserRole.TEAM_LEADER || 
               this.role == UserRole.HR || 
               this.role == UserRole.DIRECTOR;
    }

    /**
     * Retourne le nom complet de l'utilisateur
     */
    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    // ===== IMPLÉMENTATION UserDetails =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + this.role.name())
        );
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    // ===== MÉTHODES UTILITAIRES =====

    /**
     * Met à jour la date de dernière connexion
     */
    public void updateLastLoginDate() {
        this.lastLoginDate = LocalDateTime.now();
    }

    /**
     * Met à jour la date de changement de mot de passe
     */
    public void updatePasswordChangedDate() {
        this.passwordLastChanged = LocalDateTime.now();
    }

    /**
     * Marque l'email comme vérifié
     */
    public void markEmailAsVerified() {
        this.emailVerifiedAt = LocalDateTime.now();
    }

}
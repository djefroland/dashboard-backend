// src/main/java/com/dashboard/backend/service/user/UserProfileService.java
package com.dashboard.backend.service.user;

import com.dashboard.backend.dto.user.ChangePasswordRequest;
import com.dashboard.backend.dto.user.UpdateProfileRequest;
import com.dashboard.backend.dto.user.UserProfileDto;
import com.dashboard.backend.entity.user.User;
import com.dashboard.backend.exception.custom.ResourceNotFoundException;
import com.dashboard.backend.exception.custom.UnauthorizedActionException;
import com.dashboard.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ValidationException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Obtient le profil de l'utilisateur connecté
     */
    public UserProfileDto getCurrentUserProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        return mapToUserProfileDto(user);
    }

    /**
     * Met à jour le profil de l'utilisateur
     */
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Vérifier si l'email est déjà utilisé par un autre utilisateur
        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Cette adresse email est déjà utilisée");
        }

        // Mise à jour des informations
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        // Si l'email a changé, marquer comme non vérifié
        if (!user.getEmail().equals(request.getEmail())) {
            user.setEmailVerifiedAt(null);
        }

        User updatedUser = userRepository.save(user);
        
        log.info("Profil mis à jour pour l'utilisateur: {} (ID: {})", 
                user.getUsername(), user.getId());

        return mapToUserProfileDto(updatedUser);
    }

    /**
     * Change le mot de passe de l'utilisateur
     */
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedActionException("L'ancien mot de passe est incorrect");
        }

        // Vérifier que les nouveaux mots de passe correspondent
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Les mots de passe ne correspondent pas");
        }

        // Vérifier que le nouveau mot de passe est différent de l'ancien
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ValidationException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        // Valider la force du mot de passe (optionnel - peut utiliser PasswordPolicyValidator)
        validatePasswordStrength(request.getNewPassword());

        // Mettre à jour le mot de passe
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.updatePasswordChangedDate();
        userRepository.save(user);

        log.info("Mot de passe changé pour l'utilisateur: {} (ID: {})", 
                user.getUsername(), user.getId());
    }

    /**
     * Valide la force d'un mot de passe
     */
    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new ValidationException("Le mot de passe doit contenir au moins 8 caractères");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new ValidationException("Le mot de passe doit contenir au moins une majuscule");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new ValidationException("Le mot de passe doit contenir au moins une minuscule");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new ValidationException("Le mot de passe doit contenir au moins un chiffre");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new ValidationException("Le mot de passe doit contenir au moins un caractère spécial");
        }
    }

    /**
     * Mappe un User vers un UserProfileDto
     */
    private UserProfileDto mapToUserProfileDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .roleDisplayName(user.getRole().getDisplayName())
                .departmentId(user.getDepartmentId())
                .managerId(user.getManagerId())
                .enabled(user.getEnabled())
                .requiresTimeTracking(user.getRequiresTimeTracking())
                .lastLoginDate(user.getLastLoginDate())
                .createdAt(user.getCreatedAt())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .canManageEmployees(user.getRole().canManageEmployees())
                .canApproveLeaves(user.getRole().canApproveLeaves())
                .canViewGlobalStats(user.getRole().canViewGlobalStats())
                .canExportData(user.getRole().canExportData())
                .build();
    }
}
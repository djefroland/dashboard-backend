// src/main/java/com/dashboard/backend/service/user/UserRequestService.java
package com.dashboard.backend.service.user;

import com.dashboard.backend.dto.user.CreateUserRequest;
import com.dashboard.backend.dto.user.ReviewUserRequestDto;
import com.dashboard.backend.dto.user.UserRequestDto;
import com.dashboard.backend.entity.user.User;
import com.dashboard.backend.entity.user.UserRequest;
import com.dashboard.backend.exception.custom.ResourceNotFoundException;
import com.dashboard.backend.exception.custom.UnauthorizedActionException;
import com.dashboard.backend.repository.UserRepository;
import com.dashboard.backend.repository.UserRequestRepository;
import com.dashboard.backend.security.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ValidationException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserRequestService {

    private final UserRequestRepository userRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crée une demande de création d'utilisateur (par RH)
     */
    public UserRequestDto createUserRequest(CreateUserRequest request, Long requesterId) {
        // Vérifier que le demandeur est RH ou Directeur
        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur demandeur non trouvé"));

        if (!requester.getRole().canManageEmployees()) {
            throw new UnauthorizedActionException("Seuls les RH et Directeurs peuvent créer des demandes d'utilisateur");
        }

        // Vérifications d'unicité
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("Ce nom d'utilisateur existe déjà");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Cette adresse email existe déjà");
        }

        if (userRequestRepository.existsByUsernameAndStatus(request.getUsername(), UserRequest.UserRequestStatus.PENDING)) {
            throw new ValidationException("Une demande avec ce nom d'utilisateur est déjà en attente");
        }

        if (userRequestRepository.existsByEmailAndStatus(request.getEmail(), UserRequest.UserRequestStatus.PENDING)) {
            throw new ValidationException("Une demande avec cette adresse email est déjà en attente");
        }

        // Génération d'un mot de passe temporaire
        String temporaryPassword = generateTemporaryPassword();

        // Création de la demande
        UserRequest userRequest = UserRequest.builder()
                .requestType(UserRequest.UserRequestType.CREATE_USER)
                .status(UserRequest.UserRequestStatus.PENDING)
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .requestedRole(request.getRole())
                .departmentId(request.getDepartmentId())
                .managerId(request.getManagerId())
                .requestedById(requesterId)
                .justification(request.getJustification())
                .temporaryPassword(passwordEncoder.encode(temporaryPassword))
                .build();

        UserRequest saved = userRequestRepository.save(userRequest);

        log.info("Demande de création d'utilisateur créée: {} par {} (ID: {})",
                request.getUsername(), requester.getUsername(), requesterId);

        return mapToUserRequestDto(saved);
    }

    /**
     * Obtient toutes les demandes en attente (pour Directeur)
     */
    public List<UserRequestDto> getPendingRequests() {
        List<UserRequest> pendingRequests = userRequestRepository.findByStatus(UserRequest.UserRequestStatus.PENDING);
        return pendingRequests.stream()
                .map(this::mapToUserRequestDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtient les demandes avec pagination
     */
    public Page<UserRequestDto> getRequests(UserRequest.UserRequestStatus status, Pageable pageable) {
        Page<UserRequest> requests;
        if (status != null) {
            requests = userRequestRepository.findByStatus(status, pageable);
        } else {
            requests = userRequestRepository.findAll(pageable);
        }
        return requests.map(this::mapToUserRequestDto);
    }

    /**
     * Approuve ou rejette une demande (par Directeur)
     */
    public UserRequestDto reviewRequest(Long requestId, ReviewUserRequestDto reviewDto, Long reviewerId) {
        UserRequest request = userRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée"));

        // Vérifier que le reviewer est Directeur
        User reviewer = userRepository.findById(reviewerId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur reviewer non trouvé"));

        if (reviewer.getRole() != UserRole.DIRECTOR) {
            throw new UnauthorizedActionException("Seul le Directeur peut approuver/rejeter les demandes");
        }

        if (request.getStatus() != UserRequest.UserRequestStatus.PENDING) {
            throw new ValidationException("Cette demande a déjà été traitée");
        }

        // Mise à jour de la demande
        request.setStatus(reviewDto.getDecision());
        request.setReviewedById(reviewerId);
        request.setReviewDate(LocalDateTime.now());
        request.setReviewComments(reviewDto.getComments());

        // Si approuvé, créer l'utilisateur
        if (reviewDto.getDecision() == UserRequest.UserRequestStatus.APPROVED) {
            User createdUser = createUserFromRequest(request);
            request.setUserCreatedId(createdUser.getId());
            
            log.info("Utilisateur créé suite à approbation: {} (ID: {})", 
                    createdUser.getUsername(), createdUser.getId());
        }

        UserRequest updated = userRequestRepository.save(request);

        log.info("Demande {} {} par le Directeur {} (ID: {})",
                reviewDto.getDecision() == UserRequest.UserRequestStatus.APPROVED ? "approuvée" : "rejetée",
                requestId, reviewer.getUsername(), reviewerId);

        return mapToUserRequestDto(updated);
    }

    /**
     * Crée un utilisateur à partir d'une demande approuvée
     */
    private User createUserFromRequest(UserRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getTemporaryPassword()) // Déjà encodé
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(request.getRequestedRole())
                .departmentId(request.getDepartmentId())
                .managerId(request.getManagerId())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(false) // Forcer le changement du mot de passe temporaire
                .requiresTimeTracking(request.getRequestedRole().requiresTimeTracking())
                .build();

        return userRepository.save(user);
    }

    /**
     * Génère un mot de passe temporaire sécurisé
     */
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }

    /**
     * Annule une demande (par le créateur ou un Directeur)
     */
    public void cancelRequest(Long requestId, Long userId) {
        UserRequest request = userRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Vérifier les autorisations
        boolean canCancel = request.getRequestedById().equals(userId) || 
                           user.getRole() == UserRole.DIRECTOR;

        if (!canCancel) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à annuler cette demande");
        }

        if (request.getStatus() != UserRequest.UserRequestStatus.PENDING) {
            throw new ValidationException("Seules les demandes en attente peuvent être annulées");
        }

        request.setStatus(UserRequest.UserRequestStatus.CANCELLED);
        userRequestRepository.save(request);

        log.info("Demande {} annulée par {} (ID: {})", requestId, user.getUsername(), userId);
    }

    /**
     * Obtient les statistiques des demandes
     */
    public Map<String, Object> getRequestsStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("pending", userRequestRepository.countByStatus(UserRequest.UserRequestStatus.PENDING));
        stats.put("approved", userRequestRepository.countByStatus(UserRequest.UserRequestStatus.APPROVED));
        stats.put("rejected", userRequestRepository.countByStatus(UserRequest.UserRequestStatus.REJECTED));
        stats.put("cancelled", userRequestRepository.countByStatus(UserRequest.UserRequestStatus.CANCELLED));
        
        return stats;
    }

    /**
     * Mappe UserRequest vers UserRequestDto
     */
    private UserRequestDto mapToUserRequestDto(UserRequest request) {
        // Récupération des noms pour l'affichage
        String requestedByName = userRepository.findById(request.getRequestedById())
            .map(User::getFullName).orElse("Utilisateur inconnu");

        String reviewedByName = request.getReviewedById() != null ?
            userRepository.findById(request.getReviewedById())
                .map(User::getFullName).orElse("Utilisateur inconnu") : null;

        return UserRequestDto.builder()
                .id(request.getId())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .fullName(request.getFirstName() + " " + request.getLastName())
                .phone(request.getPhone())
                .requestedRole(request.getRequestedRole())
                .requestedRoleDisplayName(request.getRequestedRole().getDisplayName())
                .departmentId(request.getDepartmentId())
                .managerId(request.getManagerId())
                .requestedById(request.getRequestedById())
                .requestedByName(requestedByName)
                .reviewedById(request.getReviewedById())
                .reviewedByName(reviewedByName)
                .reviewDate(request.getReviewDate())
                .reviewComments(request.getReviewComments())
                .justification(request.getJustification())
                .createdAt(request.getCreatedAt())
                .userCreatedId(request.getUserCreatedId())
                .build();
    }
}
package com.dashboard.backend.service.auth;

import com.dashboard.backend.dto.auth.LoginRequest;
import com.dashboard.backend.dto.auth.LoginResponse;
import com.dashboard.backend.entity.user.User;
import com.dashboard.backend.exception.custom.UnauthorizedActionException;
import com.dashboard.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service principal d'authentification
 * Gère la connexion, déconnexion et validation des utilisateurs
 */
@Service
@Slf4j
@Transactional
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;

    @Value("${app.jwt.expiration}")
    private Long jwtExpirationMs;
    
    // Utilisation de @Autowired au lieu de @RequiredArgsConstructor pour contrôler l'injection
    @Autowired
    public AuthService(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }
    
    // Injection de AuthenticationManager séparément avec @Lazy pour briser la dépendance circulaire
    @Autowired
    public void setAuthenticationManager(@Lazy AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Authentification d'un utilisateur
     */
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            log.info("Tentative de connexion pour l'utilisateur: {}", loginRequest.getIdentifier());

            // 1. Authentification avec Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getIdentifier(),
                    loginRequest.getPassword()
                )
            );

            // 2. Récupération de l'utilisateur authentifié
            User user = findUserByIdentifier(loginRequest.getIdentifier())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

            // 3. Vérifications supplémentaires
            validateUserStatus(user);

            // 4. Génération des tokens
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // 5. Mise à jour de la dernière connexion
            user.updateLastLoginDate();
            userRepository.save(user);

            // 6. Création de la réponse
            LoginResponse response = LoginResponse.fromUser(
                user, 
                accessToken, 
                refreshToken, 
                jwtExpirationMs / 1000
            );

            log.info("Connexion réussie pour l'utilisateur: {} (ID: {})", user.getUsername(), user.getId());
            return response;

        } catch (BadCredentialsException e) {
            log.warn("Échec de connexion - Identifiants incorrects pour: {}", loginRequest.getIdentifier());
            throw new UnauthorizedActionException("Identifiants incorrects");
        } catch (DisabledException e) {
            log.warn("Échec de connexion - Compte désactivé pour: {}", loginRequest.getIdentifier());
            throw new UnauthorizedActionException("Compte désactivé");
        } catch (AuthenticationException e) {
            log.error("Erreur d'authentification pour: {} - {}", loginRequest.getIdentifier(), e.getMessage());
            throw new UnauthorizedActionException("Échec de l'authentification");
        }
    }

    /**
     * Rafraîchissement d'un token
     */
    public LoginResponse refreshToken(String refreshToken) {
        try {
            // 1. Validation du refresh token
            if (!jwtService.validateToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
                throw new UnauthorizedActionException("Token de rafraîchissement invalide");
            }

            // 2. Extraction des informations utilisateur
            String username = jwtService.extractUsername(refreshToken);
            User user = findUserByIdentifier(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

            // 3. Vérifications
            validateUserStatus(user);

            // 4. Génération de nouveaux tokens
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            // 5. Création de la réponse
            LoginResponse response = LoginResponse.fromUser(
                user, 
                newAccessToken, 
                newRefreshToken, 
                jwtExpirationMs / 1000
            );

            log.info("Token rafraîchi pour l'utilisateur: {}", user.getUsername());
            return response;

        } catch (Exception e) {
            log.error("Erreur lors du rafraîchissement du token: {}", e.getMessage());
            throw new UnauthorizedActionException("Impossible de rafraîchir le token");
        }
    }

    /**
     * Déconnexion d'un utilisateur
     * Note: Avec JWT stateless, la déconnexion côté serveur est optionnelle
     */
    public void logout(String token) {
        try {
            if (jwtService.validateToken(token)) {
                String username = jwtService.extractUsername(token);
                log.info("Déconnexion de l'utilisateur: {}", username);
                
                // Ici on pourrait ajouter le token à une blacklist si nécessaire
                // Pour l'instant, on se contente de logger
            }
        } catch (Exception e) {
            log.warn("Erreur lors de la déconnexion: {}", e.getMessage());
        }
    }

    /**
     * Validation du statut d'un utilisateur
     */
    private void validateUserStatus(User user) {
        if (!user.isEnabled()) {
            throw new UnauthorizedActionException("Compte désactivé");
        }
        if (!user.isAccountNonLocked()) {
            throw new UnauthorizedActionException("Compte verrouillé");
        }
        if (!user.isAccountNonExpired()) {
            throw new UnauthorizedActionException("Compte expiré");
        }
        if (!user.isCredentialsNonExpired()) {
            throw new UnauthorizedActionException("Mot de passe expiré");
        }
    }

    /**
     * Recherche d'un utilisateur par identifiant (username ou email)
     */
    private Optional<User> findUserByIdentifier(String identifier) {
        return userRepository.findByUsernameOrEmail(identifier);
    }

    /**
     * Implémentation de UserDetailsService pour Spring Security
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        log.debug("Chargement de l'utilisateur: {}", identifier);
        
        return findUserByIdentifier(identifier)
            .orElseThrow(() -> {
                log.warn("Utilisateur non trouvé: {}", identifier);
                return new UsernameNotFoundException("Utilisateur non trouvé: " + identifier);
            });
    }

    /**
     * Vérification de la validité d'un token
     */
    public boolean isTokenValid(String token) {
        try {
            return jwtService.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extraction des informations utilisateur depuis un token
     */
    public User getUserFromToken(String token) {
        String username = jwtService.extractUsername(token);
        return findUserByIdentifier(username)
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
    }

    /**
     * Changement de mot de passe (nécessite l'ancien mot de passe)
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        // Vérification de l'ancien mot de passe
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new UnauthorizedActionException("Ancien mot de passe incorrect");
        }

        // Mise à jour du mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        user.updatePasswordChangedDate();
        userRepository.save(user);

        log.info("Mot de passe changé pour l'utilisateur: {}", user.getUsername());
    }

    /**
     * Vérifie si c'est la première connexion de l'utilisateur
     */
    public boolean isFirstLogin(User user) {
        return user.getLastLoginDate() == null;
    }
}
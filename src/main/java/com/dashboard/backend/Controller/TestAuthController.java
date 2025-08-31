package com.dashboard.backend.Controller;

import com.dashboard.backend.entity.user.User;
import com.dashboard.backend.repository.UserRepository;
import com.dashboard.backend.service.auth.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Contrôleur de test pour vérifier le service JWT
 * À UTILISER UNIQUEMENT POUR LE DÉBOGAGE
 */
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Slf4j
public class TestAuthController {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Test de génération directe d'un token JWT
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> testGenerateToken(@RequestParam String username) {
        try {
            // Recherche de l'utilisateur par username
            Optional<User> userOpt = userRepository.findByUsernameOrEmail(username);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Utilisateur non trouvé",
                    "username", username
                ));
            }
            
            User user = userOpt.get();
            
            // Génération directe du token avec le service JWT
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtService.getExpirationTime(accessToken));
            response.put("username", user.getUsername());
            response.put("role", user.getRole().name());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors du test JWT: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
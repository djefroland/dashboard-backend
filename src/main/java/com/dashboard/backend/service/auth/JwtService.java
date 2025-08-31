package com.dashboard.backend.service.auth;

import com.dashboard.backend.entity.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service pour la gestion des tokens JWT
 * Génération, validation et extraction d'informations des tokens
 */
@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private Long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration}")
    private Long refreshExpirationMs;

    @Value("${app.jwt.issuer}")
    private String jwtIssuer;

    /**
     * Génère la clé secrète pour signer les tokens
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Génère un token d'accès pour un utilisateur
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        claims.put("fullName", user.getFullName());
        claims.put("requiresTimeTracking", user.shouldTrackTime());
        
        return createToken(claims, user.getUsername(), jwtExpirationMs);
    }

    /**
     * Génère un token de rafraîchissement
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("tokenType", "refresh");
        
        return createToken(claims, user.getUsername(), refreshExpirationMs);
    }

    /**
     * Crée un token JWT avec les claims spécifiés
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(jwtIssuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrait le nom d'utilisateur du token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrait l'ID utilisateur du token
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * Extrait le rôle du token
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extrait l'email du token
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    /**
     * Extrait la date d'expiration du token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrait un claim spécifique du token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrait tous les claims du token
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expiré: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("Token JWT non supporté: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("Token JWT malformé: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string est vide: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de l'extraction des claims: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Vérifie si le token est expiré
     */
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Valide le token pour un utilisateur donné
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Erreur lors de la validation du token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Valide le token sans vérifier l'utilisateur
     */
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Token invalide: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si c'est un token de rafraîchissement
     */
    public Boolean isRefreshToken(String token) {
        try {
            String tokenType = extractClaim(token, claims -> claims.get("tokenType", String.class));
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtient le temps restant avant expiration (en secondes)
     */
    public Long getExpirationTime(String token) {
        try {
            Date expiration = extractExpiration(token);
            long currentTime = System.currentTimeMillis();
            long expirationTime = expiration.getTime();
            return Math.max(0, (expirationTime - currentTime) / 1000);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Extrait toutes les informations utilisateur du token
     */
    public Map<String, Object> extractUserInfo(String token) {
        Claims claims = extractAllClaims(token);
        Map<String, Object> userInfo = new HashMap<>();
        
        userInfo.put("username", claims.getSubject());
        userInfo.put("userId", claims.get("userId"));
        userInfo.put("role", claims.get("role"));
        userInfo.put("email", claims.get("email"));
        userInfo.put("fullName", claims.get("fullName"));
        userInfo.put("requiresTimeTracking", claims.get("requiresTimeTracking"));
        userInfo.put("issuedAt", claims.getIssuedAt());
        userInfo.put("expiration", claims.getExpiration());
        
        return userInfo;
    }

    /**
     * Vérifie si le token peut être rafraîchi (pas trop expiré)
     */
    public Boolean canRefreshToken(String token) {
        try {
            Date expiration = extractExpiration(token);
            // Permet le rafraîchissement jusqu'à 7 jours après expiration
            long gracePeriod = 7 * 24 * 60 * 60 * 1000; // 7 jours en ms
            return expiration.getTime() + gracePeriod > System.currentTimeMillis();
        } catch (Exception e) {
            return false;
        }
    }
}
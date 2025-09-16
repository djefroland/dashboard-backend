package com.dashboard.backend.config.filter;

import com.dashboard.backend.service.auth.AuthService;
import com.dashboard.backend.service.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT pour l'authentification automatique
 * Intercepte chaque requête pour vérifier et valider le token JWT
 * Utilise l'injection tardive pour éviter les dépendances circulaires
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    // Cette méthode permet d'obtenir AuthService de manière paresseuse
    // pour éviter la dépendance circulaire lors du démarrage
    private AuthService getAuthService() {
        return applicationContext.getBean(AuthService.class);
    }

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. Extraction du token JWT de la requête
            String jwt = extractJwtFromRequest(request);

            // 2. Si un token est présent et valide
            if (jwt != null && jwtService.validateToken(jwt)) {
                
                // 3. Extraction du nom d'utilisateur
                String username = jwtService.extractUsername(jwt);

                // 4. Si l'utilisateur n'est pas déjà authentifié dans le contexte de sécurité
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    
                    // 5. Chargement des détails de l'utilisateur (utilise la méthode paresseuse)
                    UserDetails userDetails = getAuthService().loadUserByUsername(username);

                    // 6. Validation du token avec les détails de l'utilisateur
                    if (jwtService.validateToken(jwt, userDetails)) {
                        
                        // 7. Création du token d'authentification Spring Security
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                            );

                        // 8. Ajout des détails de la requête
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // 9. Définition de l'authentification dans le contexte de sécurité
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        log.debug("Utilisateur authentifié: {} avec les rôles: {}", 
                            username, userDetails.getAuthorities());
                    } else {
                        log.debug("Token JWT invalide pour l'utilisateur: {}", username);
                    }
                } else {
                    log.debug("Utilisateur déjà authentifié ou nom d'utilisateur null");
                }
            } else {
                log.debug("Pas de token JWT valide trouvé dans la requête");
            }

        } catch (Exception e) {
            log.error("Erreur lors de l'authentification JWT: {}", e.getMessage(), e);
            // Nettoyer le contexte de sécurité en cas d'erreur
            SecurityContextHolder.clearContext();
        }

        // 10. Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }

    /**
     * Extrait le token JWT de l'header Authorization
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            log.debug("Token JWT extrait de l'header Authorization");
            return token;
        }
        
        return null;
    }

    /**
     * Détermine si ce filtre doit être appliqué à cette requête
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Ne pas appliquer le filtre aux endpoints publics
        boolean shouldNotFilter = path.contains("/auth/") ||
               path.contains("/swagger-ui") ||
               path.contains("/api-docs") ||
               path.contains("/actuator/health") ||
               path.contains("/actuator/info") ||
               path.contains("/test/");
               
        if (shouldNotFilter) {
            log.debug("JWT filter bypassed for public endpoint: {}", path);
        }
        
        return shouldNotFilter;
    }
}
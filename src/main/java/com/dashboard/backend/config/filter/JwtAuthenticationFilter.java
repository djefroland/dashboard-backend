package com.dashboard.backend.config.filter;

import com.dashboard.backend.service.auth.AuthService;
import com.dashboard.backend.service.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthService authService;

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
                    
                    // 5. Chargement des détails de l'utilisateur
                    UserDetails userDetails = authService.loadUserByUsername(username);

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
                    }
                }
            }

        } catch (Exception e) {
            log.error("Erreur lors de l'authentification JWT: {}", e.getMessage());
            // Ne pas bloquer la requête, laisser Spring Security gérer l'accès non autorisé
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
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * Détermine si ce filtre doit être appliqué à cette requête
     * Peut être surchargé pour exclure certains endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String servletPath = request.getServletPath();
        
        log.info("Checking path for JWT filter - URI: {}, ServletPath: {}", path, servletPath);
        
        // Ne pas appliquer le filtre aux endpoints publics
        boolean shouldNotFilter = path.contains("/auth/") ||
               path.contains("/swagger-ui") ||
               path.contains("/api-docs") ||
               path.contains("/actuator/health") ||
               path.contains("/actuator/info") ||
               path.contains("/test/");
               
        log.info("JWT filter should not filter this request: {} for path: {}", shouldNotFilter, path);
        
        return shouldNotFilter;
    }
}
package com.dashboard.backend.config;

import com.dashboard.backend.config.filter.JwtAuthenticationFilter;
import com.dashboard.backend.service.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration Spring Security pour l'authentification JWT
 * Basée sur les rôles du diagramme de cas d'utilisation
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private AuthService authService;
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }
    
    @Autowired
    public void setJwtAuthenticationEntryPoint(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    /**
     * Encoder pour les mots de passe
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Filtre JWT personnalisé
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * Provider d'authentification DAO
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(authService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Manager d'authentification
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configuration CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origines autorisées (frontend)
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:4200", 
            "http://localhost:5173",
            "https://dashboard-rh-frontend.vercel.app"
        ));
        
        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Headers autorisés
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Autoriser les credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Headers exposés au client
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "Accept", "X-Requested-With"
        ));
        
        // Durée de cache pour les requêtes preflight
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configuration principale de sécurité
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Désactiver CSRF (pas nécessaire pour JWT)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configuration CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configuration des autorisations
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics (authentification)
                .requestMatchers("/api/v1/auth/**").permitAll()
                
                // Endpoints de documentation
                .requestMatchers("/api/v1/swagger-ui/**").permitAll()
                .requestMatchers("/api/v1/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                
                // Endpoints de monitoring
                .requestMatchers("/api/v1/actuator/health").permitAll()
                .requestMatchers("/api/v1/actuator/info").permitAll()
                
                // Endpoints pour tests de développement
                .requestMatchers("/api/v1/test/**").permitAll()
                
                // Endpoints spécifiques par rôle
                
                // DIRECTEUR - Accès total
                .requestMatchers("/api/v1/admin/**").hasRole("DIRECTOR")
                .requestMatchers("/api/v1/dashboard/global-stats").hasRole("DIRECTOR")
                .requestMatchers("/api/v1/employees/validate").hasRole("DIRECTOR")
                .requestMatchers("/api/v1/users/change-role").hasRole("DIRECTOR")
                
                // RH - Gestion des employés et approbation finale
                .requestMatchers("/api/v1/employees/create").hasAnyRole("DIRECTOR", "HR")
                .requestMatchers("/api/v1/employees/manage").hasAnyRole("DIRECTOR", "HR")
                .requestMatchers("/api/v1/leaves/final-approval").hasAnyRole("DIRECTOR", "HR")
                .requestMatchers("/api/v1/dashboard/export").hasAnyRole("DIRECTOR", "HR")
                
                // TEAM_LEADER - Gestion d'équipe
                .requestMatchers("/api/v1/leaves/first-approval").hasAnyRole("DIRECTOR", "HR", "TEAM_LEADER")
                .requestMatchers("/api/v1/performance/**").hasAnyRole("DIRECTOR", "HR", "TEAM_LEADER")
                .requestMatchers("/api/v1/reports/team").hasAnyRole("DIRECTOR", "HR", "TEAM_LEADER")
                
                // EMPLOYEE & INTERN - Fonctionnalités de base
                .requestMatchers("/api/v1/attendance/clock-in").hasAnyRole("EMPLOYEE", "INTERN", "TEAM_LEADER")
                .requestMatchers("/api/v1/leaves/request").hasAnyRole("EMPLOYEE", "INTERN", "TEAM_LEADER")
                .requestMatchers("/api/v1/reports/individual").hasAnyRole("EMPLOYEE", "INTERN", "TEAM_LEADER")
                
                // Endpoints communs à tous les utilisateurs connectés
                .requestMatchers("/api/v1/profile/**").authenticated()
                .requestMatchers("/api/v1/announcements/view").authenticated()
                .requestMatchers("/api/v1/notifications/**").authenticated()
                
                // Toutes les autres requêtes nécessitent une authentification
                .anyRequest().authenticated()
            )
            
            // Point d'entrée pour les erreurs d'authentification
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            
            // Politique de session stateless (JWT)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Provider d'authentification
            .authenticationProvider(authenticationProvider())
            
            // Filtre JWT avant le filtre d'authentification standard
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
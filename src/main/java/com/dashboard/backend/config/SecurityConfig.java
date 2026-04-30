package com.dashboard.backend.config;

import com.dashboard.backend.config.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.http.HttpMethod;

import java.util.Arrays;

/**
 * Configuration Spring Security avec injection correcte du filtre JWT

 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {


    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final DaoAuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter; // ✅ INJECTION DIRECTE

    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            DaoAuthenticationProvider authenticationProvider,
            JwtAuthenticationFilter jwtAuthenticationFilter) { // ✅ AJOUTER ICI
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.authenticationProvider = authenticationProvider;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter; // ✅ INITIALISER
    }

    /**
     * Configuration CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();


        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:5173",
                "https://dashboard-rh-frontend.vercel.app"));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept", "X-Requested-With"));
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

                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .requestMatchers("/test/**").permitAll()

                        // Endpoints spécifiques par rôle
                        .requestMatchers("/admin/**").hasRole("DIRECTOR")
                        .requestMatchers("/dashboard/global-stats").hasRole("DIRECTOR")
                        .requestMatchers("/employees/validate").hasRole("DIRECTOR")
                        .requestMatchers("/users/change-role").hasRole("DIRECTOR")

                        .requestMatchers("/employees/create").hasAnyRole("DIRECTOR", "HR")
                        .requestMatchers("/employees/manage").hasAnyRole("DIRECTOR", "HR")
                        .requestMatchers("/leaves/final-approval").hasAnyRole("DIRECTOR", "HR")
                        .requestMatchers("/dashboard/export").hasAnyRole("DIRECTOR", "HR")

                        .requestMatchers("/leaves/first-approval").hasAnyRole("DIRECTOR", "HR", "TEAM_LEADER")
                        .requestMatchers("/performance/**").hasAnyRole("DIRECTOR", "HR", "TEAM_LEADER")
                        .requestMatchers("/reports/team").hasAnyRole("DIRECTOR", "HR", "TEAM_LEADER")

                        .requestMatchers("/attendance/clock-in").hasAnyRole("EMPLOYEE", "INTERN", "TEAM_LEADER")
                        .requestMatchers("/leaves/request").hasAnyRole("EMPLOYEE", "INTERN", "TEAM_LEADER")
                        .requestMatchers("/reports/individual").hasAnyRole("EMPLOYEE", "INTERN", "TEAM_LEADER")

                        .requestMatchers("/users/**").authenticated()
                        .requestMatchers("/announcements/view").authenticated()
                        .requestMatchers("/notifications/**").authenticated()
                        .requestMatchers("/attendance/**").authenticated()

                        .requestMatchers("/reports/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/reports").hasAnyRole("TEAM_LEADER", "HR", "DIRECTOR")
                        .requestMatchers(HttpMethod.DELETE, "/reports/**").hasAnyRole("HR", "DIRECTOR")
                        .requestMatchers(HttpMethod.POST, "/notifications")
                        .hasAnyRole("TEAM_LEADER", "HR", "DIRECTOR")
                        .requestMatchers(HttpMethod.POST, "/notifications/system").hasAnyRole("HR", "DIRECTOR")

                        .anyRequest().authenticated())

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // ✅ UTILISER
                                                                                                       // L'INSTANCE
                                                                                                       // INJECTÉE


        return http.build();
    }
}
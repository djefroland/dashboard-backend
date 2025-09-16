package com.dashboard.backend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Configuration des propriétés JWT
 * Charge automatiquement les propriétés depuis application.properties
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    
    /**
     * Clé secrète pour signer les tokens JWT
     */
    private String secret;
    
    /**
     * Durée de validité du token d'accès en millisecondes
     */
    private Long expiration;
    
    /**
     * Durée de validité du token de rafraîchissement en millisecondes
     */
    private Long refreshExpiration;
    
    /**
     * Émetteur du token JWT
     */
    private String issuer;
}

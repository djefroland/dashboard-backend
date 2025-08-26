package com.dashboard.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la demande de connexion
 * Accepte username ou email comme identifiant
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "L'identifiant est obligatoire")
    @Size(min = 3, max = 100, message = "L'identifiant doit contenir entre 3 et 100 caractères")
    private String identifier; // username ou email

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    // Option pour "Se souvenir de moi"
    @Builder.Default
    private Boolean rememberMe = false;
}
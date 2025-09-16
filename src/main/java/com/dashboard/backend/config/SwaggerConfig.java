package com.dashboard.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI attendanceAPI() {
        return new OpenAPI()
                .addTagsItem(new Tag().name("Gestion des Présences").description("APIs pour le pointage et suivi des présences"));
    }
}
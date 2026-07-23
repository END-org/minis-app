package com.end.minisbackend.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata and Firebase ID token bearer security scheme. */
@Configuration
public class OpenApiConfig {
  private static final String FIREBASE_BEARER_SCHEME = "firebase-id-token";

  @Bean
  OpenAPI minisOpenApi() {
    SecurityScheme bearerScheme =
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("Firebase ID token")
            .description("Paste a Firebase Authentication ID token as the bearer value.");
    return new OpenAPI()
        .info(
            new Info()
                .title("MINIS API")
                .version("v1")
                .description("Backend API for the MINIS mobile-first mini-film platform."))
        .components(new Components().addSecuritySchemes(FIREBASE_BEARER_SCHEME, bearerScheme))
        .addSecurityItem(new SecurityRequirement().addList(FIREBASE_BEARER_SCHEME));
  }
}

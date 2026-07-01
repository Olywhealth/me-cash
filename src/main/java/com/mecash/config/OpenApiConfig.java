package com.mecash.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI document served by SpringDoc.
 *
 * <p>Swagger UI is available at {@code /swagger-ui.html} and the raw spec at
 * {@code /v3/api-docs}; both are whitelisted in {@link SecurityConfig}.
 *
 * <p>A single {@code bearerAuth} scheme (HTTP bearer, JWT) is declared and applied
 * globally so the Swagger UI "Authorize" button lets you paste a token obtained from
 * {@code POST /api/v1/auth/login}. The auth endpoints themselves are public, so the
 * requirement is only enforced by Spring Security, not by the documentation.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI mecashOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mecash API")
                        .description("Money transfer service: sign up, view accounts and balances, "
                                + "and transfer funds across currencies.")
                        .version("v1")
                        .contact(new Contact().name("Mecash"))
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT returned by POST /api/v1/auth/login "
                                        + "(without the \"Bearer \" prefix).")));
    }
}
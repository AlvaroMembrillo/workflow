package com.alvaro.workflow.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Añade a Swagger UI el botón "Authorize" para probar los endpoints protegidos con un token.
 */
@Configuration
@OpenAPIDefinition(
		info = @Info(title = "Workflow API", version = "v1", description = "API REST del portal de empleo Workflow"),
		security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
@SecurityScheme(name = OpenApiConfig.ESQUEMA_JWT, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class OpenApiConfig {

	static final String ESQUEMA_JWT = "bearerAuth";

}

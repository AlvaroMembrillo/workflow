package com.alvaro.workflow.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

/**
 * @param origenesPermitidos orígenes (por ejemplo el frontend Angular) que pueden llamar a la API
 */
@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(@NotEmpty List<String> origenesPermitidos) {
}

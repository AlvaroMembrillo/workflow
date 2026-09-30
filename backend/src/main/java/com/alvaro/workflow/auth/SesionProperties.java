package com.alvaro.workflow.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/**
 * @param duracion tiempo sin usar la web tras el que hay que volver a iniciar sesión; cada renovación lo reinicia
 * @param margenDeReutilizacion tiempo durante el que un token recién canjeado se sigue aceptando, porque dos
 *        pestañas pueden renovar a la vez con la misma cookie
 * @param cookieSegura la cookie solo se envía por HTTPS. Solo se desactiva en local, donde se usa HTTP
 */
@Validated
@ConfigurationProperties(prefix = "app.sesion")
public record SesionProperties(@NotNull Duration duracion, @NotNull Duration margenDeReutilizacion,
		boolean cookieSegura) {
}

package com.alvaro.workflow.config;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @param issuer valor del claim {@code iss}; los tokens con otro emisor se rechazan
 * @param duracionToken tiempo de validez de cada token de acceso
 * @param clavePrivada fichero PEM (PKCS#8) con la clave RSA privada que firma los tokens
 * @param generarClaveSiNoExiste crea la clave si el fichero no existe. Solo para desarrollo
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		@NotBlank String issuer,
		@NotNull Duration duracionToken,
		@NotNull(message = "indica la ruta de la clave privada con la variable de entorno JWT_CLAVE_PRIVADA") Path clavePrivada,
		boolean generarClaveSiNoExiste) {
}

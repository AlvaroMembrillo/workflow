package com.alvaro.workflow.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/**
 * @param validezVerificacion tiempo durante el que vale el enlace para verificar el email
 * @param validezRestablecimiento tiempo durante el que vale el enlace para cambiar la contraseña
 */
@Validated
@ConfigurationProperties(prefix = "app.enlaces")
public record EnlacesProperties(@NotNull Duration validezVerificacion, @NotNull Duration validezRestablecimiento) {

	Duration validez(TipoDeToken tipo) {
		return switch (tipo) {
			case VERIFICAR_EMAIL -> validezVerificacion;
			case RESTABLECER_PASSWORD -> validezRestablecimiento;
		};
	}

}

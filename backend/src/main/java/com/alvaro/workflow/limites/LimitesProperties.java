package com.alvaro.workflow.limites;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cuántas veces se puede repetir una acción en un intervalo de tiempo.
 *
 * @param fallosDeLogin contraseñas incorrectas seguidas para un mismo email desde una misma dirección IP
 * @param loginPorIp inicios de sesión, correctos o no, desde una misma IP
 * @param registroPorIp cuentas creadas desde una misma IP
 * @param correosPorDestinatario correos de recuperación o de verificación enviados a una misma cuenta
 * @param correosPorIp correos de recuperación pedidos desde una misma IP
 */
@Validated
@ConfigurationProperties(prefix = "app.limites")
public record LimitesProperties(
		@NotNull @Valid Limite fallosDeLogin,
		@NotNull @Valid Limite loginPorIp,
		@NotNull @Valid Limite registroPorIp,
		@NotNull @Valid Limite correosPorDestinatario,
		@NotNull @Valid Limite correosPorIp) {

	/**
	 * @param maximo veces que se permite la acción dentro de la ventana
	 * @param ventana tiempo que tiene que pasar desde la primera vez para volver a empezar a contar
	 */
	public record Limite(@Min(1) int maximo, @NotNull Duration ventana) {
	}

}

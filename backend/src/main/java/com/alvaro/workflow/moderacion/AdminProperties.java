package com.alvaro.workflow.moderacion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cuenta de administrador que se crea al arrancar. Las cuentas ADMIN no se pueden crear desde el registro.
 *
 * @param email si está vacío, no se crea ninguna
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password) {

	boolean configurada() {
		return email != null && !email.isBlank();
	}

}

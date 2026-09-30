package com.alvaro.workflow.correo;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @param remitente dirección que aparece como remitente, por ejemplo {@code Workflow <no-responder@ejemplo.com>}
 * @param urlPublica dirección de la web, para construir los enlaces de los correos
 * @param contacto dirección de las personas que atienden el portal: recibe las denuncias y se da como
 *        contacto en los correos de moderación
 */
@Validated
@ConfigurationProperties(prefix = "app.correo")
public record CorreoProperties(@NotBlank String remitente, @NotNull URI urlPublica, @NotBlank String contacto) {

	/** Dirección completa de una página de la web, por ejemplo {@code enlace("/mis-candidaturas")}. */
	public String enlace(String ruta) {
		String base = urlPublica.toString();
		return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + ruta;
	}

}

package com.alvaro.workflow.usuario;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Lo que el usuario puede cambiar de su cuenta. Solo se cambia lo que se envía.
 *
 * @param avisosPorCorreo si quiere recibir avisos por correo; null para dejarlo como está
 * @param nombre nombre nuevo; null para dejarlo como está
 */
public record PreferenciasRequest(
		Boolean avisosPorCorreo,

		@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
		@Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
		String nombre) {
}

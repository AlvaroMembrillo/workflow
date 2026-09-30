package com.alvaro.workflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioDePasswordRequest(
		@NotBlank(message = "Escribe tu contraseña actual") String passwordActual,

		@NotBlank(message = "La contraseña es obligatoria")
		@Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
		String passwordNueva) {
}

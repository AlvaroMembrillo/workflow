package com.alvaro.workflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecuperacionRequest(
		@NotBlank(message = "El email es obligatorio")
		@Email(message = "El email no tiene un formato válido")
		String email) {
}

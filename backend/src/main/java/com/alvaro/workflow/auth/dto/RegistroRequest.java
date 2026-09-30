package com.alvaro.workflow.auth.dto;

import com.alvaro.workflow.usuario.Rol;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
		@NotBlank(message = "El email es obligatorio")
		@Email(message = "El email no tiene un formato válido")
		@Size(max = 255, message = "El email no puede superar los 255 caracteres")
		String email,

		// BCrypt solo tiene en cuenta los primeros 72 bytes de la contraseña
		@NotBlank(message = "La contraseña es obligatoria")
		@Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
		String password,

		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
		String nombre,

		@NotNull(message = "El rol es obligatorio")
		@Schema(allowableValues = { "CANDIDATO", "EMPRESA" })
		Rol rol,

		// @AssertTrue da por bueno un null, así que también hace falta @NotNull
		@NotNull(message = "Tienes que aceptar las condiciones de uso y la política de privacidad")
		@AssertTrue(message = "Tienes que aceptar las condiciones de uso y la política de privacidad")
		@Schema(description = "El usuario ha leído y acepta las condiciones de uso y la política de privacidad")
		Boolean aceptaCondiciones) {
}

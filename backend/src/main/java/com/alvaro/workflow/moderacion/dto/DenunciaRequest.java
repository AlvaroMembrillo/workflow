package com.alvaro.workflow.moderacion.dto;

import com.alvaro.workflow.moderacion.MotivoDenuncia;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DenunciaRequest(
		@NotNull(message = "Elige un motivo") MotivoDenuncia motivo,

		@Size(max = 1000, message = "La explicación no puede superar los 1000 caracteres")
		String detalle) {
}

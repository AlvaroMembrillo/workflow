package com.alvaro.workflow.candidatura.dto;

import com.alvaro.workflow.candidatura.EstadoCandidatura;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoCandidaturaRequest(
		@NotNull(message = "El estado es obligatorio")
		@Schema(allowableValues = { "EN_REVISION", "ACEPTADA", "RECHAZADA" },
				description = "RETIRADA solo lo puede aplicar el candidato, con POST /api/candidaturas/{id}/retirada")
		EstadoCandidatura estado) {
}

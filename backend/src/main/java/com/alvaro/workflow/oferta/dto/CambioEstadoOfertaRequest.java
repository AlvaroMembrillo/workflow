package com.alvaro.workflow.oferta.dto;

import com.alvaro.workflow.oferta.EstadoOferta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoOfertaRequest(
		@NotNull(message = "El estado es obligatorio")
		@Schema(allowableValues = { "ABIERTA", "CERRADA" })
		EstadoOferta estado) {
}

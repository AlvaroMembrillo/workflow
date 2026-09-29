package com.alvaro.workflow.candidatura.dto;

import java.time.Instant;
import java.util.UUID;

import com.alvaro.workflow.candidatura.EstadoCandidatura;
import com.alvaro.workflow.oferta.dto.OfertaResumen;

/** Una candidatura vista por el candidato que la envió. */
public record MiCandidaturaResponse(
		UUID id,
		OfertaResumen oferta,
		EstadoCandidatura estado,
		String cartaPresentacion,
		Instant fechaCreacion,
		Instant fechaActualizacion) {
}

package com.alvaro.workflow.candidatura.dto;

import java.time.Instant;
import java.util.UUID;

import com.alvaro.workflow.candidatura.EstadoCandidatura;

/** Una candidatura vista por la empresa que publicó la oferta. */
public record CandidaturaRecibidaResponse(
		UUID id,
		CandidatoResumen candidato,
		EstadoCandidatura estado,
		String cartaPresentacion,
		Instant fechaCreacion,
		Instant fechaActualizacion) {
}

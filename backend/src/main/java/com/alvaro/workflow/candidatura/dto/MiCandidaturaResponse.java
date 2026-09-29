package com.alvaro.workflow.candidatura.dto;

import java.time.Instant;
import java.util.UUID;

import com.alvaro.workflow.candidatura.EstadoCandidatura;
import com.alvaro.workflow.oferta.dto.OfertaResumen;

/**
 * Una candidatura vista por el candidato que la envió.
 *
 * @param fechaRevision cuándo pasó a revisión; null si no ha pasado por ese estado
 * @param fechaResolucion cuándo se decidió o se retiró; null mientras siga abierta
 */
public record MiCandidaturaResponse(
		UUID id,
		OfertaResumen oferta,
		EstadoCandidatura estado,
		String cartaPresentacion,
		Instant fechaCreacion,
		Instant fechaRevision,
		Instant fechaResolucion,
		Instant fechaActualizacion) {
}

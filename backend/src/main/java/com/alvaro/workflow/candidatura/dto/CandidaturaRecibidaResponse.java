package com.alvaro.workflow.candidatura.dto;

import java.time.Instant;
import java.util.UUID;

import com.alvaro.workflow.candidatura.EstadoCandidatura;
import com.alvaro.workflow.cv.CurriculumResponse;

/**
 * Una candidatura vista por la empresa que publicó la oferta.
 *
 * @param cv el currículum del candidato, que se descarga con GET /api/candidaturas/{id}/cv; null si no ha subido
 */
public record CandidaturaRecibidaResponse(
		UUID id,
		CandidatoResumen candidato,
		EstadoCandidatura estado,
		String cartaPresentacion,
		CurriculumResponse cv,
		Instant fechaCreacion,
		Instant fechaRevision,
		Instant fechaResolucion,
		Instant fechaActualizacion) {
}

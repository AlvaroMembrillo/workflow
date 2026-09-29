package com.alvaro.workflow.candidatura.dto;

import java.util.EnumMap;
import java.util.Map;

import com.alvaro.workflow.candidatura.EstadoCandidatura;

/** Cuántas candidaturas tiene una oferta en cada estado. */
public record ResumenCandidaturas(long total, long pendientes, long enRevision, long aceptadas, long rechazadas,
		long retiradas) {

	public static final ResumenCandidaturas VACIO = new ResumenCandidaturas(0, 0, 0, 0, 0, 0);

	public static ResumenCandidaturas de(Map<EstadoCandidatura, Long> porEstado) {
		Map<EstadoCandidatura, Long> recuento = new EnumMap<>(EstadoCandidatura.class);
		recuento.putAll(porEstado);
		long pendientes = recuento.getOrDefault(EstadoCandidatura.PENDIENTE, 0L);
		long enRevision = recuento.getOrDefault(EstadoCandidatura.EN_REVISION, 0L);
		long aceptadas = recuento.getOrDefault(EstadoCandidatura.ACEPTADA, 0L);
		long rechazadas = recuento.getOrDefault(EstadoCandidatura.RECHAZADA, 0L);
		long retiradas = recuento.getOrDefault(EstadoCandidatura.RETIRADA, 0L);
		return new ResumenCandidaturas(pendientes + enRevision + aceptadas + rechazadas + retiradas, pendientes,
				enRevision, aceptadas, rechazadas, retiradas);
	}

}

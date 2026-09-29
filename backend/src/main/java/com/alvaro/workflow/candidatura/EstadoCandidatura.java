package com.alvaro.workflow.candidatura;

/**
 * Estados de una candidatura. La empresa la hace avanzar: PENDIENTE → EN_REVISION → ACEPTADA o RECHAZADA.
 * Puede decidir sin pasar por EN_REVISION, pero una decisión final ya no se puede cambiar.
 */
public enum EstadoCandidatura {

	PENDIENTE,
	EN_REVISION,
	ACEPTADA,
	RECHAZADA;

	public boolean puedeCambiarA(EstadoCandidatura nuevo) {
		return switch (this) {
			case PENDIENTE -> nuevo == EN_REVISION || nuevo == ACEPTADA || nuevo == RECHAZADA;
			case EN_REVISION -> nuevo == ACEPTADA || nuevo == RECHAZADA;
			case ACEPTADA, RECHAZADA -> false;
		};
	}

}

package com.alvaro.workflow.candidatura;

/**
 * Estados de una candidatura. La empresa la hace avanzar: PENDIENTE → EN_REVISION → ACEPTADA o RECHAZADA,
 * y puede decidir sin pasar por EN_REVISION. El candidato puede retirarla mientras no haya decisión.
 * Los estados finales ya no se pueden cambiar.
 */
public enum EstadoCandidatura {

	PENDIENTE,
	EN_REVISION,
	ACEPTADA,
	RECHAZADA,
	RETIRADA;

	public boolean puedeCambiarA(EstadoCandidatura nuevo) {
		return switch (this) {
			case PENDIENTE -> nuevo == EN_REVISION || nuevo == ACEPTADA || nuevo == RECHAZADA || nuevo == RETIRADA;
			case EN_REVISION -> nuevo == ACEPTADA || nuevo == RECHAZADA || nuevo == RETIRADA;
			case ACEPTADA, RECHAZADA, RETIRADA -> false;
		};
	}

	public boolean esFinal() {
		return this == ACEPTADA || this == RECHAZADA || this == RETIRADA;
	}

}

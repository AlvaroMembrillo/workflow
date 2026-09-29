package com.alvaro.workflow.candidatura;

import com.alvaro.workflow.common.ConflictoException;

public class CambioDeEstadoNoPermitidoException extends ConflictoException {

	public CambioDeEstadoNoPermitidoException(EstadoCandidatura actual, EstadoCandidatura nuevo) {
		super("Cambio de estado no permitido", "Una candidatura en estado " + actual + " no puede pasar a " + nuevo);
	}

}

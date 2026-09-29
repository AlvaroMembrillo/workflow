package com.alvaro.workflow.candidatura;

import com.alvaro.workflow.common.ConflictoException;

public class OfertaCerradaException extends ConflictoException {

	public OfertaCerradaException() {
		super("Oferta cerrada", "La oferta ya no admite candidaturas");
	}

}

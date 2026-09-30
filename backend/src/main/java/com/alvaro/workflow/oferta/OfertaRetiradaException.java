package com.alvaro.workflow.oferta;

import com.alvaro.workflow.common.ConflictoException;

public class OfertaRetiradaException extends ConflictoException {

	public OfertaRetiradaException() {
		super("Oferta retirada", "Hemos retirado esta oferta por incumplir las normas de publicación: no se puede modificar");
	}

}

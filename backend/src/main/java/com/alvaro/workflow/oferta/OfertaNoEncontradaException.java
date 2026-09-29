package com.alvaro.workflow.oferta;

import java.util.UUID;

import com.alvaro.workflow.common.RecursoNoEncontradoException;

public class OfertaNoEncontradaException extends RecursoNoEncontradoException {

	public OfertaNoEncontradaException(UUID id) {
		super("Oferta no encontrada", "No existe ninguna oferta con id " + id);
	}

}

package com.alvaro.workflow.candidatura;

import java.util.UUID;

import com.alvaro.workflow.common.RecursoNoEncontradoException;

public class CandidaturaNoEncontradaException extends RecursoNoEncontradoException {

	public CandidaturaNoEncontradaException(UUID id) {
		super("Candidatura no encontrada", "No existe ninguna candidatura con id " + id);
	}

}

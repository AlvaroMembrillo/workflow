package com.alvaro.workflow.common;

/** Se responde con 404. */
public class RecursoNoEncontradoException extends ErrorDeNegocioException {

	public RecursoNoEncontradoException(String titulo, String detalle) {
		super(titulo, detalle);
	}

}

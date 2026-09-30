package com.alvaro.workflow.common;

/** La petición necesita una sesión válida y no la trae. Se responde con 401. */
public class NoAutenticadoException extends ErrorDeNegocioException {

	public NoAutenticadoException(String titulo, String detalle) {
		super(titulo, detalle);
	}

}

package com.alvaro.workflow.common;

/** El usuario tiene el rol adecuado pero el recurso no es suyo. Se responde con 403. */
public class AccesoDenegadoException extends ErrorDeNegocioException {

	public AccesoDenegadoException(String detalle) {
		super("Acceso denegado", detalle);
	}

}

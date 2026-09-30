package com.alvaro.workflow.common;

/** El usuario tiene el rol adecuado pero no puede hacer esto: el recurso no es suyo o le falta un requisito. Se responde con 403. */
public class AccesoDenegadoException extends ErrorDeNegocioException {

	public AccesoDenegadoException(String detalle) {
		this("Acceso denegado", detalle);
	}

	protected AccesoDenegadoException(String titulo, String detalle) {
		super(titulo, detalle);
	}

}

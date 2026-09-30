package com.alvaro.workflow.auth;

import com.alvaro.workflow.common.NoAutenticadoException;

public class SesionNoValidaException extends NoAutenticadoException {

	public SesionNoValidaException() {
		super("Sesión no válida", "La sesión ha caducado o se ha cerrado. Vuelve a entrar.");
	}

}

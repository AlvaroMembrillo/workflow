package com.alvaro.workflow.auth;

import com.alvaro.workflow.usuario.Rol;

public class RolNoPermitidoException extends RuntimeException {

	public RolNoPermitidoException(Rol rol) {
		super("No se puede registrar una cuenta con el rol " + rol);
	}

}

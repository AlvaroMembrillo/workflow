package com.alvaro.workflow.auth;

import com.alvaro.workflow.common.PeticionNoValidaException;
import com.alvaro.workflow.usuario.Rol;

public class RolNoPermitidoException extends PeticionNoValidaException {

	public RolNoPermitidoException(Rol rol) {
		super("Rol no permitido", "No se puede registrar una cuenta con el rol " + rol);
	}

}

package com.alvaro.workflow.auth;

import com.alvaro.workflow.common.PeticionNoValidaException;

public class EnlaceNoValidoException extends PeticionNoValidaException {

	public EnlaceNoValidoException() {
		super("Enlace no válido", "El enlace no es válido, ha caducado o ya se ha usado. Pide uno nuevo.");
	}

}

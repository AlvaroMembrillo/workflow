package com.alvaro.workflow.auth;

import com.alvaro.workflow.common.AccesoDenegadoException;

public class CuentaSuspendidaException extends AccesoDenegadoException {

	public CuentaSuspendidaException(String contacto) {
		super("Cuenta suspendida", "Hemos suspendido esta cuenta por incumplir las normas de uso. "
				+ "Si crees que es un error, escribe a " + contacto + ".");
	}

}

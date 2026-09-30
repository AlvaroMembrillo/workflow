package com.alvaro.workflow.auth;

import com.alvaro.workflow.common.AccesoDenegadoException;

/** La acción necesita que el usuario haya confirmado su email. Se responde con 403. */
public class EmailSinVerificarException extends AccesoDenegadoException {

	public EmailSinVerificarException(String paraQue) {
		super("Email sin confirmar", "Confirma tu email para " + paraQue
				+ ". Te enviamos un enlace al registrarte; puedes pedir otro en \"Mi cuenta\".");
	}

}

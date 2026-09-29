package com.alvaro.workflow.common;

/** La petición es correcta pero choca con el estado actual de los datos. Se responde con 409. */
public class ConflictoException extends ErrorDeNegocioException {

	public ConflictoException(String titulo, String detalle) {
		super(titulo, detalle);
	}

}

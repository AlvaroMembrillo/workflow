package com.alvaro.workflow.common;

/** La petición incumple una regla de negocio que no se puede expresar con Bean Validation. Se responde con 400. */
public class PeticionNoValidaException extends ErrorDeNegocioException {

	public PeticionNoValidaException(String titulo, String detalle) {
		super(titulo, detalle);
	}

}

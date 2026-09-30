package com.alvaro.workflow.common;

import lombok.Getter;

/**
 * Un campo de la petición no supera una regla que solo se puede comprobar con los datos guardados
 * (por ejemplo, la contraseña actual). Se responde con 400 y el error del campo, igual que los errores
 * de validación, para que el formulario lo muestre junto al campo.
 */
@Getter
public class CampoNoValidoException extends ErrorDeNegocioException {

	private final String campo;

	public CampoNoValidoException(String campo, String mensaje) {
		super("Datos no válidos", mensaje);
		this.campo = campo;
	}

}

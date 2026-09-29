package com.alvaro.workflow.usuario;

import java.util.UUID;

public class UsuarioNoEncontradoException extends RuntimeException {

	public UsuarioNoEncontradoException(UUID id) {
		super("No existe ningún usuario con id " + id);
	}

}

package com.alvaro.workflow.usuario;

import java.util.UUID;

import com.alvaro.workflow.common.RecursoNoEncontradoException;

public class UsuarioNoEncontradoException extends RecursoNoEncontradoException {

	public UsuarioNoEncontradoException(UUID id) {
		super("Usuario no encontrado", "No existe ningún usuario con id " + id);
	}

}

package com.alvaro.workflow.empresa;

import com.alvaro.workflow.common.RecursoNoEncontradoException;

public class EmpresaNoEncontradaException extends RecursoNoEncontradoException {

	public EmpresaNoEncontradaException(String detalle) {
		super("Empresa no encontrada", detalle);
	}

}

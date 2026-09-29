package com.alvaro.workflow.common;

import lombok.Getter;

/**
 * Error esperado de la lógica de negocio. {@link GlobalExceptionHandler} elige el código HTTP según la subclase
 * y usa {@link #getTitulo()} y el mensaje como título y detalle de la respuesta.
 */
@Getter
public abstract class ErrorDeNegocioException extends RuntimeException {

	private final String titulo;

	protected ErrorDeNegocioException(String titulo, String detalle) {
		super(detalle);
		this.titulo = titulo;
	}

}

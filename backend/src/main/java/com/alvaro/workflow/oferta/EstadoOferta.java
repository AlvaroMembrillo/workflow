package com.alvaro.workflow.oferta;

public enum EstadoOferta {
	/** Visible en el listado público y admite candidaturas. */
	ABIERTA,
	/** Ya no admite candidaturas. La empresa puede volver a abrirla. */
	CERRADA,
	/** Retirada por moderación: no es visible para el público y la empresa no puede reabrirla ni editarla. */
	RETIRADA
}

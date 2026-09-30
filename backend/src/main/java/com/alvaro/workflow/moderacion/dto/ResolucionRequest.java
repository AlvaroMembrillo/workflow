package com.alvaro.workflow.moderacion.dto;

import jakarta.validation.constraints.NotNull;

public record ResolucionRequest(@NotNull(message = "Indica qué hacer con la denuncia") Accion accion) {

	public enum Accion {
		/** La oferta incumple las normas: se retira y se avisa a la empresa. */
		RETIRAR_OFERTA,
		/** La oferta es correcta: la denuncia se archiva. */
		DESESTIMAR
	}

}

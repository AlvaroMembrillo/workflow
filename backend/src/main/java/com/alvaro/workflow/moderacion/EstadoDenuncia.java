package com.alvaro.workflow.moderacion;

public enum EstadoDenuncia {
	PENDIENTE,
	/** La oferta se ha retirado. */
	ACEPTADA,
	/** Un administrador ha revisado la oferta y no incumple las normas. */
	DESESTIMADA
}

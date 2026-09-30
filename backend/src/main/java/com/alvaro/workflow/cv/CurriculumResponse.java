package com.alvaro.workflow.cv;

import java.time.Instant;

/** @param tamano en bytes */
public record CurriculumResponse(String nombreFichero, int tamano, Instant fechaSubida) {

	public static CurriculumResponse de(CurriculumResumen resumen) {
		return resumen == null ? null
				: new CurriculumResponse(resumen.nombreFichero(), resumen.tamano(), resumen.fechaSubida());
	}

}

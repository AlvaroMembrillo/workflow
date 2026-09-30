package com.alvaro.workflow.limites;

import java.time.Duration;

import com.alvaro.workflow.common.ErrorDeNegocioException;
import com.alvaro.workflow.common.TextoDeDuracion;

import lombok.Getter;

/** Se ha superado un límite de intentos. Se responde con 429 y la cabecera Retry-After. */
@Getter
public class DemasiadosIntentosException extends ErrorDeNegocioException {

	private final Duration espera;

	public DemasiadosIntentosException(Duration espera) {
		super("Demasiados intentos",
				"Has hecho demasiados intentos. Espera " + TextoDeDuracion.de(espera) + " y vuelve a probar.");
		this.espera = espera;
	}

}

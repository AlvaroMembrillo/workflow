package com.alvaro.workflow.common;

import java.time.Duration;

/** Duraciones escritas como las diría una persona, para correos y mensajes de error. */
public final class TextoDeDuracion {

	private TextoDeDuracion() {
	}

	/** "48 horas", "1 hora", "30 minutos", "1 minuto". Redondea hacia arriba: nunca promete menos espera de la real. */
	public static String de(Duration duracion) {
		if (duracion.toMinutes() >= 60 && duracion.toSecondsPart() == 0 && duracion.toMinutesPart() == 0) {
			long horas = duracion.toHours();
			return horas == 1 ? "1 hora" : horas + " horas";
		}
		long minutos = Math.max(1, (duracion.toSeconds() + 59) / 60);
		if (minutos >= 120) {
			return (minutos + 59) / 60 + " horas";
		}
		return minutos == 1 ? "1 minuto" : minutos + " minutos";
	}

}

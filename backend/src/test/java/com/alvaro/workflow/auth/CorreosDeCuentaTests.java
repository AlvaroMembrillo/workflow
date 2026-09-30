package com.alvaro.workflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class CorreosDeCuentaTests {

	@Test
	void escribeLaValidezDeLosEnlacesComoLoDiriaUnaPersona() {
		assertThat(CorreosDeCuenta.enTexto(Duration.ofHours(48))).isEqualTo("48 horas");
		assertThat(CorreosDeCuenta.enTexto(Duration.ofHours(1))).isEqualTo("1 hora");
		assertThat(CorreosDeCuenta.enTexto(Duration.ofMinutes(30))).isEqualTo("30 minutos");
		assertThat(CorreosDeCuenta.enTexto(Duration.ofMinutes(1))).isEqualTo("1 minuto");
	}

}

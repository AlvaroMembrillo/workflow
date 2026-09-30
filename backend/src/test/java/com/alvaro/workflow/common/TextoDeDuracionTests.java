package com.alvaro.workflow.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class TextoDeDuracionTests {

	@Test
	void escribeLasDuracionesComoLoDiriaUnaPersona() {
		assertThat(TextoDeDuracion.de(Duration.ofHours(48))).isEqualTo("48 horas");
		assertThat(TextoDeDuracion.de(Duration.ofHours(1))).isEqualTo("1 hora");
		assertThat(TextoDeDuracion.de(Duration.ofMinutes(30))).isEqualTo("30 minutos");
		assertThat(TextoDeDuracion.de(Duration.ofMinutes(1))).isEqualTo("1 minuto");
	}

	@Test
	void redondeaHaciaArribaParaNoPrometerMenosEsperaDeLaReal() {
		assertThat(TextoDeDuracion.de(Duration.ofSeconds(20))).isEqualTo("1 minuto");
		assertThat(TextoDeDuracion.de(Duration.ofSeconds(61))).isEqualTo("2 minutos");
		assertThat(TextoDeDuracion.de(Duration.ofMinutes(14).plusSeconds(3))).isEqualTo("15 minutos");
		assertThat(TextoDeDuracion.de(Duration.ofMinutes(90))).isEqualTo("90 minutos");
		assertThat(TextoDeDuracion.de(Duration.ofMinutes(150))).isEqualTo("3 horas");
	}

}

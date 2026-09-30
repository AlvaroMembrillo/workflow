package com.alvaro.workflow.limites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.alvaro.workflow.limites.LimitesProperties.Limite;

class LimitadorDeIntentosTests {

	private static final Limite TRES_CADA_DIEZ_MINUTOS = new Limite(3, Duration.ofMinutes(10));

	private final AtomicReference<Instant> ahora = new AtomicReference<>(Instant.parse("2026-09-30T10:00:00Z"));
	private final LimitadorDeIntentos limitador = new LimitadorDeIntentos(ahora::get);

	@Test
	void permiteHastaElMaximoYRechazaElSiguienteIndicandoCuantoFalta() {
		contar("ana", 3);
		pasan(Duration.ofMinutes(4));

		assertThatThrownBy(() -> limitador.contar("login", "ana", TRES_CADA_DIEZ_MINUTOS))
				.isInstanceOfSatisfying(DemasiadosIntentosException.class, ex -> {
					assertThat(ex.getEspera()).isEqualTo(Duration.ofMinutes(6));
					assertThat(ex.getMessage()).isEqualTo("Has hecho demasiados intentos. Espera 6 minutos y vuelve a probar.");
				});
	}

	@Test
	void cuandoTerminaLaVentanaLaCuentaVuelveAEmpezar() {
		contar("ana", 3);

		pasan(Duration.ofMinutes(10));

		assertThatCode(() -> contar("ana", 3)).doesNotThrowAnyException();
	}

	@Test
	void cadaPersonaYCadaAccionTienenSuPropiaCuenta() {
		contar("ana", 3);

		assertThatCode(() -> limitador.contar("login", "luis", TRES_CADA_DIEZ_MINUTOS)).doesNotThrowAnyException();
		assertThatCode(() -> limitador.contar("registro", "ana", TRES_CADA_DIEZ_MINUTOS)).doesNotThrowAnyException();
	}

	@Test
	void comprobarNoGastaIntentos() {
		contar("ana", 2);

		for (int i = 0; i < 10; i++) {
			limitador.comprobar("login", "ana", TRES_CADA_DIEZ_MINUTOS);
		}
		limitador.contar("login", "ana", TRES_CADA_DIEZ_MINUTOS);

		assertThatThrownBy(() -> limitador.comprobar("login", "ana", TRES_CADA_DIEZ_MINUTOS))
				.isInstanceOf(DemasiadosIntentosException.class);
	}

	@Test
	void olvidarPoneLaCuentaACero() {
		contar("ana", 3);

		limitador.olvidar("login", "ana");

		assertThat(limitador.permite("login", "ana", TRES_CADA_DIEZ_MINUTOS)).isTrue();
	}

	@Test
	void permiteDevuelveFalseEnLugarDeLanzarLaExcepcion() {
		contar("ana", 3);

		assertThat(limitador.permite("login", "ana", TRES_CADA_DIEZ_MINUTOS)).isFalse();
	}

	@Test
	void laLimpiezaBorraSoloLasVentanasTerminadas() {
		contar("ana", 1);
		pasan(Duration.ofMinutes(6));
		contar("luis", 1);
		pasan(Duration.ofMinutes(5));

		limitador.limpiar();

		assertThat(limitador.ventanasAbiertas()).isEqualTo(1);
	}

	private void contar(String quien, int veces) {
		for (int i = 0; i < veces; i++) {
			limitador.contar("login", quien, TRES_CADA_DIEZ_MINUTOS);
		}
	}

	private void pasan(Duration tiempo) {
		ahora.updateAndGet(instante -> instante.plus(tiempo));
	}

}

package com.alvaro.workflow.limites;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.alvaro.workflow.IntegrationTestBase;

/** Frenos contra la fuerza bruta y el envío masivo de correos, con los límites de application.yaml. */
class LimitesIntegrationTests extends IntegrationTestBase {

	@Test
	void trasCincoContrasenasIncorrectasSeguidasHayQueEsperarAunqueLaSiguienteSeaLaCorrecta() {
		String email = emailUnico();
		registrar(email, "CANDIDATO");
		String ip = ipUnica();
		for (int i = 0; i < 5; i++) {
			assertThat(login(ip, email, "no-es-esta")).hasStatus(HttpStatus.UNAUTHORIZED);
		}

		MvcTestResult respuesta = login(ip, email, PASSWORD);

		assertThat(respuesta).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(respuesta).bodyJson().extractingPath("$.title").isEqualTo("Demasiados intentos");
		assertThat(respuesta).bodyJson().extractingPath("$.detail")
				.isEqualTo("Has hecho demasiados intentos. Espera 15 minutos y vuelve a probar.");
		assertThat(Integer.parseInt(respuesta.getResponse().getHeader(HttpHeaders.RETRY_AFTER))).isBetween(890, 900);
	}

	@Test
	void elBloqueoEsPorEmailYDireccionAsiQueElDuenoDeLaCuentaPuedeEntrarDesdeOtraIp() {
		String email = emailUnico();
		registrar(email, "CANDIDATO");
		String atacante = ipUnica();
		for (int i = 0; i < 6; i++) {
			login(atacante, email, "no-es-esta");
		}

		assertThat(login(ipUnica(), email, PASSWORD)).hasStatusOk();
	}

	@Test
	void entrarConLaContrasenaCorrectaPoneLaCuentaDeFallosACero() {
		String email = emailUnico();
		registrar(email, "CANDIDATO");
		String ip = ipUnica();
		for (int i = 0; i < 4; i++) {
			login(ip, email, "no-es-esta");
		}
		assertThat(login(ip, email, PASSWORD)).hasStatusOk();

		for (int i = 0; i < 4; i++) {
			assertThat(login(ip, email, "no-es-esta")).hasStatus(HttpStatus.UNAUTHORIZED);
		}
		assertThat(login(ip, email, PASSWORD)).hasStatusOk();
	}

	@Test
	void desdeUnaMismaIpSoloSePuedenCrearDiezCuentasPorHora() {
		String ip = ipUnica();
		for (int i = 0; i < 10; i++) {
			assertThat(registrarDesde(ip)).hasStatus(HttpStatus.CREATED);
		}

		assertThat(registrarDesde(ip)).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(registrarDesde(ipUnica())).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void aUnaMismaCuentaSoloSeLeEnvianTresCorreosDeRecuperacionPorHoraSinDelatarQueExiste() {
		String email = emailUnico();
		registrar(email, "CANDIDATO");

		for (int i = 0; i < 5; i++) {
			// Desde direcciones distintas: el límite es de la cuenta, no de quien lo pide
			assertThat(recuperarDesde(ipUnica(), email)).hasStatus(HttpStatus.NO_CONTENT);
		}

		assertThat(buzon.para(email)).filteredOn(mensaje -> mensaje.asunto().startsWith("Cambia tu contraseña")).hasSize(3);
	}

	@Test
	void desdeUnaMismaIpSoloSePuedenPedirDiezRecuperacionesPorHora() {
		String ip = ipUnica();
		for (int i = 0; i < 10; i++) {
			assertThat(recuperarDesde(ip, emailUnico())).hasStatus(HttpStatus.NO_CONTENT);
		}

		assertThat(recuperarDesde(ip, emailUnico())).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
	}

	@Test
	void elEnlaceDeVerificacionSoloSePuedeReenviarTresVecesPorHora() {
		String email = emailUnico();
		String token = tokenDeRegistro(registrar(email, "CANDIDATO"));
		for (int i = 0; i < 3; i++) {
			assertThat(post("/api/auth/verificacion/reenvio", token, "")).hasStatus(HttpStatus.NO_CONTENT);
		}

		assertThat(post("/api/auth/verificacion/reenvio", token, "")).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
		// El del registro y los tres reenvíos
		assertThat(buzon.para(email)).hasSize(4);
	}

	private MvcTestResult login(String ip, String email, String password) {
		return postDesde(ip, "/api/auth/login", """
				{"email": "%s", "password": "%s"}
				""".formatted(email, password));
	}

	private MvcTestResult registrarDesde(String ip) {
		return postDesde(ip, "/api/auth/registro", """
				{"email": "%s", "password": "%s", "nombre": "Ana García", "rol": "CANDIDATO"}
				""".formatted(emailUnico(), PASSWORD));
	}

	private MvcTestResult recuperarDesde(String ip, String email) {
		return postDesde(ip, "/api/auth/recuperacion", """
				{"email": "%s"}
				""".formatted(email));
	}

}

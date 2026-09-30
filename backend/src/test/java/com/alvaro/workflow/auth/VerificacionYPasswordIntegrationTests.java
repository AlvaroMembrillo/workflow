package com.alvaro.workflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.alvaro.workflow.IntegrationTestBase;
import com.alvaro.workflow.correo.Mensaje;

/** Verificación del email y cambio de contraseña, siguiendo los enlaces de los correos. */
class VerificacionYPasswordIntegrationTests extends IntegrationTestBase {

	private static final String PASSWORD_NUEVA = "otra-contraseña-segura";

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void elRegistroEnviaUnEnlaceQueVerificaElEmail() {
		String email = emailUnico();
		String token = tokenDeRegistro(registrar(email, "CANDIDATO"));
		assertThat(get("/api/usuarios/me", token)).bodyJson().extractingPath("$.emailVerificado").isEqualTo(false);

		Mensaje correo = buzon.ultimoPara(email);
		assertThat(correo.asunto()).isEqualTo("Confirma tu email en Workflow");
		assertThat(correo.texto()).contains("Hola, Ana García").contains("http://localhost:4200/verificar-email#")
				.contains("caduca en 48 horas");

		assertThat(verificar(buzon.tokenDelUltimoEnlacePara(email))).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(get("/api/usuarios/me", token)).bodyJson().extractingPath("$.emailVerificado").isEqualTo(true);
	}

	@Test
	void abrirElEnlaceDeVerificacionDosVecesNoEsUnError() {
		String email = emailUnico();
		registrar(email, "CANDIDATO");
		String enlace = buzon.tokenDelUltimoEnlacePara(email);
		verificar(enlace);

		assertThat(verificar(enlace)).hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void unEnlaceDeVerificacionInventadoDevuelve400() {
		MvcTestResult respuesta = verificar("token-inventado");

		assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(respuesta).bodyJson().extractingPath("$.title").isEqualTo("Enlace no válido");
	}

	@Test
	void unEnlaceDeVerificacionCaducadoDevuelve400() {
		String email = emailUnico();
		String token = tokenDeRegistro(registrar(email, "CANDIDATO"));
		jdbc.update("""
				update tokens_de_un_uso set fecha_caducidad = now() - interval '1 minute'
				where usuario_id = (select id from usuarios where email = ?)""", email);

		assertThat(verificar(buzon.tokenDelUltimoEnlacePara(email))).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(get("/api/usuarios/me", token)).bodyJson().extractingPath("$.emailVerificado").isEqualTo(false);
	}

	@Test
	void reenviarLaVerificacionInvalidaElEnlaceAnterior() {
		String email = emailUnico();
		String token = tokenDeRegistro(registrar(email, "EMPRESA"));
		String primerEnlace = buzon.tokenDelUltimoEnlacePara(email);

		assertThat(post("/api/auth/verificacion/reenvio", token, "")).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(buzon.para(email)).hasSize(2);
		assertThat(verificar(primerEnlace)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(verificar(buzon.tokenDelUltimoEnlacePara(email))).hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void reenviarLaVerificacionConElEmailYaVerificadoNoEnviaNada() {
		String email = emailUnico();
		String token = registrarYVerificar(email, "CANDIDATO", "Ana García");

		assertThat(post("/api/auth/verificacion/reenvio", token, "")).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(buzon.para(email)).hasSize(1);
	}

	@Test
	void reenviarLaVerificacionSinSesionDevuelve401() {
		assertThat(post("/api/auth/verificacion/reenvio", null, "")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void laRecuperacionEnviaUnEnlaceDeUnSoloUsoParaCambiarLaContrasena() {
		String email = emailUnico();
		registrar(email, "CANDIDATO");

		assertThat(recuperar(email.toUpperCase())).hasStatus(HttpStatus.NO_CONTENT);

		Mensaje correo = buzon.ultimoPara(email);
		assertThat(correo.asunto()).isEqualTo("Cambia tu contraseña de Workflow");
		assertThat(correo.texto()).contains("http://localhost:4200/restablecer#").contains("caduca en 1 hora");
		String enlace = buzon.tokenDelUltimoEnlacePara(email);

		assertThat(restablecer(enlace, PASSWORD_NUEVA)).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(login(email, PASSWORD_NUEVA)).hasStatusOk();
		assertThat(login(email, PASSWORD)).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(buzon.ultimoPara(email).asunto()).isEqualTo("Has cambiado tu contraseña de Workflow");
		// El enlace ya está gastado
		assertThat(restablecer(enlace, "una-tercera-contraseña")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(login(email, PASSWORD_NUEVA)).hasStatusOk();
	}

	@Test
	void cambiarLaContrasenaConElEnlaceDelCorreoTambienVerificaElEmail() {
		String email = emailUnico();
		registrar(email, "CANDIDATO");
		recuperar(email);

		restablecer(buzon.tokenDelUltimoEnlacePara(email), PASSWORD_NUEVA);

		String token = leer(login(email, PASSWORD_NUEVA), "$.accessToken");
		assertThat(get("/api/usuarios/me", token)).bodyJson().extractingPath("$.emailVerificado").isEqualTo(true);
	}

	@Test
	void laRecuperacionRespondeIgualSiElEmailNoExisteYNoEnviaNada() {
		String email = emailUnico();

		assertThat(recuperar(email)).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(buzon.para(email)).isEmpty();
	}

	@Test
	void elEnlaceDeVerificacionNoSirveParaCambiarLaContrasena() {
		String email = emailUnico();
		registrar(email, "CANDIDATO");

		assertThat(restablecer(buzon.tokenDelUltimoEnlacePara(email), PASSWORD_NUEVA)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(login(email, PASSWORD)).hasStatusOk();
	}

	@Test
	void restablecerConUnaContrasenaCortaDevuelve400ConElErrorDelCampo() {
		String email = emailUnico();
		registrar(email, "CANDIDATO");
		recuperar(email);
		String enlace = buzon.tokenDelUltimoEnlacePara(email);

		MvcTestResult respuesta = restablecer(enlace, "corta");

		assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(respuesta).bodyJson().extractingPath("$.errores").asMap().containsOnlyKeys("password");
		// El enlace sigue valiendo: no se gasta por un error de validación
		assertThat(restablecer(enlace, PASSWORD_NUEVA)).hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void cambiarLaContrasenaConSesionExigeLaActual() {
		String email = emailUnico();
		String token = tokenDeRegistro(registrar(email, "CANDIDATO"));

		MvcTestResult conLaActualMal = post("/api/usuarios/me/password", token, """
				{"passwordActual": "no-es-esta", "passwordNueva": "%s"}
				""".formatted(PASSWORD_NUEVA));
		assertThat(conLaActualMal).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(conLaActualMal).bodyJson().extractingPath("$.errores.passwordActual")
				.isEqualTo("La contraseña actual no es correcta");

		assertThat(post("/api/usuarios/me/password", token, """
				{"passwordActual": "%s", "passwordNueva": "%s"}
				""".formatted(PASSWORD, PASSWORD_NUEVA))).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(login(email, PASSWORD_NUEVA)).hasStatusOk();
		assertThat(login(email, PASSWORD)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void elUsuarioPuedeDesactivarLosAvisosPorCorreo() {
		String token = nuevoCandidato();
		assertThat(get("/api/usuarios/me", token)).bodyJson().extractingPath("$.avisosPorCorreo").isEqualTo(true);

		MvcTestResult respuesta = patch("/api/usuarios/me", token, """
				{"avisosPorCorreo": false}
				""");

		assertThat(respuesta).hasStatusOk();
		assertThat(respuesta).bodyJson().extractingPath("$.avisosPorCorreo").isEqualTo(false);
		assertThat(get("/api/usuarios/me", token)).bodyJson().extractingPath("$.avisosPorCorreo").isEqualTo(false);
	}

	private MvcTestResult verificar(String token) {
		return post("/api/auth/verificacion", null, """
				{"token": "%s"}
				""".formatted(token));
	}

	private MvcTestResult recuperar(String email) {
		return post("/api/auth/recuperacion", null, """
				{"email": "%s"}
				""".formatted(email));
	}

	private MvcTestResult restablecer(String token, String password) {
		return post("/api/auth/restablecimiento", null, """
				{"token": "%s", "password": "%s"}
				""".formatted(token, password));
	}

	private MvcTestResult login(String email, String password) {
		return post("/api/auth/login", null, """
				{"email": "%s", "password": "%s"}
				""".formatted(email, password));
	}

}

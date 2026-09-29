package com.alvaro.workflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.alvaro.workflow.IntegrationTestBase;

/**
 * Prueba el flujo completo: registro, login y acceso a un endpoint protegido con el JWT.
 */
class AutenticacionIntegrationTests extends IntegrationTestBase {

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void registroDevuelveUnTokenConLosDatosDelUsuario() {
		String email = emailUnico();

		MvcTestResult respuesta = registrar(email, "EMPRESA");

		assertThat(respuesta).hasStatus(HttpStatus.CREATED);
		assertThat(respuesta).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
		assertThat(respuesta).bodyJson().extractingPath("$.expiresIn").isEqualTo(3600);

		Jwt jwt = jwtDecoder.decode(leer(respuesta, "$.accessToken"));
		assertThat(jwt.getClaimAsString("iss")).isEqualTo("workflow-api");
		assertThat(jwt.getClaimAsString("email")).isEqualTo(email);
		assertThat(jwt.getClaimAsStringList("roles")).containsExactly("EMPRESA");
		assertThat(UUID.fromString(jwt.getSubject())).isNotNull();
	}

	@Test
	void registroConUnEmailYaRegistradoDevuelve409AunqueCambienLasMayusculas() {
		String email = emailUnico();
		registrar(email, "CANDIDATO");

		MvcTestResult respuesta = registrar(email.toUpperCase(), "CANDIDATO");

		assertThat(respuesta).hasStatus(HttpStatus.CONFLICT);
		assertThat(respuesta).bodyJson().extractingPath("$.title").isEqualTo("Email ya registrado");
	}

	@Test
	void registroComoAdminDevuelve400() {
		assertThat(registrar(emailUnico(), "ADMIN")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void registroConCamposNoValidosDevuelve400ConElErrorDeCadaCampo() {
		MvcTestResult respuesta = post("/api/auth/registro", null, """
				{"email": "no-es-un-email", "password": "corta", "nombre": "", "rol": "CANDIDATO"}
				""");

		assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(respuesta).bodyJson().extractingPath("$.errores").asMap()
				.containsOnlyKeys("email", "password", "nombre");
	}

	@Test
	void loginConCredencialesCorrectasDevuelveUnToken() {
		String email = emailUnico();
		registrar(email, "CANDIDATO");

		MvcTestResult respuesta = login("  " + email.toUpperCase() + " ", PASSWORD);

		assertThat(respuesta).hasStatusOk();
		assertThat(jwtDecoder.decode(leer(respuesta, "$.accessToken")).getClaimAsString("email")).isEqualTo(email);
	}

	@Test
	void loginConPasswordIncorrectaDevuelve401() {
		String email = emailUnico();
		registrar(email, "CANDIDATO");

		MvcTestResult respuesta = login(email, "otra-contraseña");

		assertThat(respuesta).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(respuesta).bodyJson().extractingPath("$.title").isEqualTo("Credenciales incorrectas");
	}

	@Test
	void loginConEmailNoRegistradoDevuelveLaMismaRespuestaQueConPasswordIncorrecta() {
		MvcTestResult respuesta = login(emailUnico(), PASSWORD);

		assertThat(respuesta).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(respuesta).bodyJson().extractingPath("$.title").isEqualTo("Credenciales incorrectas");
	}

	@Test
	void meConTokenDevuelveLosDatosDelUsuario() {
		String email = emailUnico();
		String token = leer(registrar(email, "CANDIDATO"), "$.accessToken");

		MvcTestResult respuesta = get("/api/usuarios/me", token);

		assertThat(respuesta).hasStatusOk();
		assertThat(respuesta).bodyJson().extractingPath("$.email").isEqualTo(email);
		assertThat(respuesta).bodyJson().extractingPath("$.nombre").isEqualTo("Ana García");
		assertThat(respuesta).bodyJson().extractingPath("$.rol").isEqualTo("CANDIDATO");
	}

	@Test
	void meSinTokenDevuelve401() {
		assertThat(get("/api/usuarios/me", null)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void tokenManipuladoParaCambiarElRolDevuelve401() {
		String[] partes = nuevoCandidato().split("\\.");
		String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
		String payloadManipulado = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payload.replace("CANDIDATO", "ADMIN").getBytes(StandardCharsets.UTF_8));

		MvcTestResult respuesta = get("/api/usuarios/me", partes[0] + "." + payloadManipulado + "." + partes[2]);

		assertThat(respuesta).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void swaggerYHealthSonPublicos() {
		assertThat(get("/v3/api-docs", null)).hasStatusOk();
		assertThat(get("/actuator/health", null)).hasStatusOk();
	}

	@Test
	void corsPermiteElFrontendYRechazaOtrosOrigenes() {
		MvcTestResult frontend = preflight("http://localhost:4200");
		assertThat(frontend).hasStatusOk();
		assertThat(frontend.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
				.isEqualTo("http://localhost:4200");

		assertThat(preflight("https://otro-sitio.example")).hasStatus(HttpStatus.FORBIDDEN);
	}

	private MvcTestResult login(String email, String password) {
		return post("/api/auth/login", null, """
				{"email": "%s", "password": "%s"}
				""".formatted(email, password));
	}

	private MvcTestResult preflight(String origen) {
		return mvc.options().uri("/api/auth/login")
				.header(HttpHeaders.ORIGIN, origen)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.exchange();
	}

}

package com.alvaro.workflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.alvaro.workflow.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;

/**
 * Prueba el flujo completo contra PostgreSQL real: registro, login y acceso a un endpoint protegido con el JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AutenticacionIntegrationTests {

	private static final String PASSWORD = "contraseña-segura";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void registroDevuelveUnTokenConLosDatosDelUsuario() throws Exception {
		String email = emailUnico();

		MvcTestResult respuesta = registrar(email, "EMPRESA");

		assertThat(respuesta).hasStatus(HttpStatus.CREATED);
		assertThat(respuesta).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
		assertThat(respuesta).bodyJson().extractingPath("$.expiresIn").isEqualTo(3600);

		Jwt jwt = jwtDecoder.decode(token(respuesta));
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
		MvcTestResult respuesta = mvc.post().uri("/api/auth/registro")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "no-es-un-email", "password": "corta", "nombre": "", "rol": "CANDIDATO"}
						""")
				.exchange();

		assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(respuesta).bodyJson().extractingPath("$.errores").asMap()
				.containsOnlyKeys("email", "password", "nombre");
	}

	@Test
	void loginConCredencialesCorrectasDevuelveUnToken() throws Exception {
		String email = emailUnico();
		registrar(email, "CANDIDATO");

		MvcTestResult respuesta = login("  " + email.toUpperCase() + " ", PASSWORD);

		assertThat(respuesta).hasStatusOk();
		assertThat(jwtDecoder.decode(token(respuesta)).getClaimAsString("email")).isEqualTo(email);
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
	void meConTokenDevuelveLosDatosDelUsuario() throws Exception {
		String email = emailUnico();
		String token = token(registrar(email, "CANDIDATO"));

		MvcTestResult respuesta = mvc.get().uri("/api/usuarios/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.exchange();

		assertThat(respuesta).hasStatusOk();
		assertThat(respuesta).bodyJson().extractingPath("$.email").isEqualTo(email);
		assertThat(respuesta).bodyJson().extractingPath("$.nombre").isEqualTo("Ana García");
		assertThat(respuesta).bodyJson().extractingPath("$.rol").isEqualTo("CANDIDATO");
	}

	@Test
	void meSinTokenDevuelve401() {
		assertThat(mvc.get().uri("/api/usuarios/me").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void tokenManipuladoParaCambiarElRolDevuelve401() throws Exception {
		String[] partes = token(registrar(emailUnico(), "CANDIDATO")).split("\\.");
		String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
		String payloadManipulado = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payload.replace("CANDIDATO", "ADMIN").getBytes(StandardCharsets.UTF_8));

		MvcTestResult respuesta = mvc.get().uri("/api/usuarios/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + partes[0] + "." + payloadManipulado + "." + partes[2])
				.exchange();

		assertThat(respuesta).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void swaggerYHealthSonPublicos() {
		assertThat(mvc.get().uri("/v3/api-docs").exchange()).hasStatusOk();
		assertThat(mvc.get().uri("/actuator/health").exchange()).hasStatusOk();
	}

	@Test
	void corsPermiteElFrontendYRechazaOtrosOrigenes() {
		MvcTestResult frontend = preflight("http://localhost:4200");
		assertThat(frontend).hasStatusOk();
		assertThat(frontend.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
				.isEqualTo("http://localhost:4200");

		assertThat(preflight("https://otro-sitio.example")).hasStatus(HttpStatus.FORBIDDEN);
	}

	private MvcTestResult registrar(String email, String rol) {
		return mvc.post().uri("/api/auth/registro")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "%s", "password": "%s", "nombre": "Ana García", "rol": "%s"}
						""".formatted(email, PASSWORD, rol))
				.exchange();
	}

	private MvcTestResult login(String email, String password) {
		return mvc.post().uri("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "%s", "password": "%s"}
						""".formatted(email, password))
				.exchange();
	}

	private MvcTestResult preflight(String origen) {
		return mvc.options().uri("/api/auth/login")
				.header(HttpHeaders.ORIGIN, origen)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.exchange();
	}

	private static String token(MvcTestResult respuesta) throws Exception {
		return JsonPath.read(respuesta.getResponse().getContentAsString(), "$.accessToken");
	}

	/** Cada test usa su propio email para no depender del orden ni de los datos de otros tests. */
	private static String emailUnico() {
		return "usuario-" + UUID.randomUUID() + "@test.com";
	}

}

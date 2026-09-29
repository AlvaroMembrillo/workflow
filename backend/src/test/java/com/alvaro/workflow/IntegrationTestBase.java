package com.alvaro.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

/**
 * Base de los tests de integración: aplicación completa contra PostgreSQL real (Testcontainers).
 * Todas las subclases comparten el mismo contexto de Spring y la misma base de datos, así que
 * cada test crea sus propios usuarios y datos en lugar de depender de los de otros tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {

	protected static final String PASSWORD = "contraseña-segura";

	@Autowired
	protected MockMvcTester mvc;

	protected MvcTestResult registrar(String email, String rol) {
		return registrar(email, rol, "Ana García");
	}

	protected MvcTestResult registrar(String email, String rol, String nombre) {
		return post("/api/auth/registro", null, """
				{"email": "%s", "password": "%s", "nombre": "%s", "rol": "%s"}
				""".formatted(email, PASSWORD, nombre, rol));
	}

	/** Registra una empresa llamada "Empresa de prueba" y devuelve su token. */
	protected String nuevaEmpresa() {
		return tokenDeRegistro(registrar(emailUnico(), "EMPRESA", "Empresa de prueba"));
	}

	/** Registra un candidato llamado "Ana García" y devuelve su token. */
	protected String nuevoCandidato() {
		return tokenDeRegistro(registrar(emailUnico(), "CANDIDATO"));
	}

	protected MvcTestResult get(String uri, String token) {
		return conToken(mvc.get().uri(uri), token).exchange();
	}

	protected MvcTestResult post(String uri, String token, String json) {
		return conToken(mvc.post().uri(uri), token).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	protected MvcTestResult put(String uri, String token, String json) {
		return conToken(mvc.put().uri(uri), token).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	protected MvcTestResult patch(String uri, String token, String json) {
		return conToken(mvc.patch().uri(uri), token).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	/** Lee un valor del JSON de la respuesta, por ejemplo {@code leer(respuesta, "$.id")}. */
	protected static <T> T leer(MvcTestResult respuesta, String jsonPath) {
		try {
			return JsonPath.read(respuesta.getResponse().getContentAsString(), jsonPath);
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected static String emailUnico() {
		return "usuario-" + UUID.randomUUID() + "@test.com";
	}

	/** Texto aleatorio para identificar los datos de un test al buscarlos entre los de otros tests. */
	protected static String marcaUnica() {
		return "m" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

	private static String tokenDeRegistro(MvcTestResult respuesta) {
		assertThat(respuesta).hasStatus(HttpStatus.CREATED);
		return leer(respuesta, "$.accessToken");
	}

	private static MockMvcRequestBuilder conToken(MockMvcRequestBuilder peticion, String token) {
		return token == null ? peticion : peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

}

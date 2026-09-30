package com.alvaro.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import com.alvaro.workflow.correo.BuzonDePrueba;
import com.alvaro.workflow.correo.CorreoDePruebaConfiguration;
import com.jayway.jsonpath.JsonPath;

/**
 * Base de los tests de integración: aplicación completa contra PostgreSQL real (Testcontainers).
 * Todas las subclases comparten el mismo contexto de Spring y la misma base de datos, así que
 * cada test crea sus propios usuarios y datos en lugar de depender de los de otros tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, CorreoDePruebaConfiguration.class })
public abstract class IntegrationTestBase {

	protected static final String PASSWORD = "contraseña-segura";

	@Autowired
	protected MockMvcTester mvc;

	@Autowired
	protected BuzonDePrueba buzon;

	protected MvcTestResult registrar(String email, String rol) {
		return registrar(email, rol, "Ana García");
	}

	protected MvcTestResult registrar(String email, String rol, String nombre) {
		return post("/api/auth/registro", null, """
				{"email": "%s", "password": "%s", "nombre": "%s", "rol": "%s", "aceptaCondiciones": true}
				""".formatted(email, PASSWORD, nombre, rol));
	}

	/** Registra una empresa llamada "Empresa de prueba", con el email verificado, y devuelve su token. */
	protected String nuevaEmpresa() {
		return registrarYVerificar(emailUnico(), "EMPRESA", "Empresa de prueba");
	}

	/** Registra un candidato llamado "Ana García", con el email verificado, y devuelve su token. */
	protected String nuevoCandidato() {
		return registrarYVerificar(emailUnico(), "CANDIDATO", "Ana García");
	}

	/** Registra una cuenta y verifica su email con el enlace del correo. Devuelve su token. */
	protected String registrarYVerificar(String email, String rol, String nombre) {
		String token = tokenDeRegistro(registrar(email, rol, nombre));
		assertThat(post("/api/auth/verificacion", null, """
				{"token": "%s"}
				""".formatted(buzon.tokenDelUltimoEnlacePara(email)))).hasStatus(HttpStatus.NO_CONTENT);
		return token;
	}

	/** Sube un currículum como lo hace el formulario de la web. */
	protected MvcTestResult subirCv(String token, String nombre, byte[] contenido) {
		return conToken(mvc.post().uri("/api/candidatos/me/cv"), token).multipart()
				.file(new MockMultipartFile("fichero", nombre, MediaType.APPLICATION_PDF_VALUE, contenido))
				.exchange();
	}

	/** Un fichero que empieza como un PDF, que es lo que comprueba la API. */
	protected static byte[] pdf(String texto) {
		return ("%PDF-1.4\n" + texto).getBytes(StandardCharsets.UTF_8);
	}

	protected MvcTestResult delete(String uri, String token) {
		return conToken(mvc.delete().uri(uri), token).exchange();
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

	protected static String tokenDeRegistro(MvcTestResult respuesta) {
		assertThat(respuesta).hasStatus(HttpStatus.CREATED);
		return leer(respuesta, "$.accessToken");
	}

	/**
	 * Cada petición llega desde una dirección IP distinta, para que los límites por IP no hagan fallar
	 * tests que no tienen que ver con ellos. Los tests de los límites fijan la IP con {@code desde}.
	 */
	private static <B extends AbstractMockHttpServletRequestBuilder<B>> B conToken(B peticion, String token) {
		peticion.remoteAddress(ipUnica());
		return token == null ? peticion : peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

	/** Una petición POST con JSON que llega desde la IP indicada. */
	protected MvcTestResult postDesde(String ip, String uri, String json) {
		return mvc.post().uri(uri).remoteAddress(ip).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	protected static String ipUnica() {
		ThreadLocalRandom azar = ThreadLocalRandom.current();
		return "10.%d.%d.%d".formatted(azar.nextInt(256), azar.nextInt(256), azar.nextInt(1, 255));
	}

}

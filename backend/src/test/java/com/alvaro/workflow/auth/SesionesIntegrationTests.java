package com.alvaro.workflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.alvaro.workflow.IntegrationTestBase;

import jakarta.servlet.http.Cookie;

/** Sesiones largas con el token de refresco en una cookie HttpOnly. */
class SesionesIntegrationTests extends IntegrationTestBase {

	private static final String COOKIE = "workflow_refresco";

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void entrarDejaElTokenDeRefrescoEnUnaCookieQueElJavaScriptNoPuedeLeer() {
		String email = emailUnico();
		MvcTestResult registro = registrar(email, "CANDIDATO");
		MvcTestResult login = login(email);

		for (MvcTestResult respuesta : new MvcTestResult[] { registro, login }) {
			MockCookie cookie = (MockCookie) respuesta.getResponse().getCookie(COOKIE);
			assertThat(cookie).isNotNull();
			assertThat(cookie.getValue()).hasSizeGreaterThan(40);
			assertThat(cookie.isHttpOnly()).isTrue();
			assertThat(cookie.getSameSite()).isEqualTo("Strict");
			assertThat(cookie.getPath()).isEqualTo("/api/auth");
			assertThat(cookie.getMaxAge()).isEqualTo(30 * 24 * 60 * 60);
			// El token de refresco no aparece en el cuerpo, que sí puede leer el JavaScript
			assertThat(respuesta.getResponse().getContentAsByteArray()).asString().doesNotContain(cookie.getValue());
		}
	}

	@Test
	void elTokenDeRefrescoSeCanjeaPorOtroTokenDeAccesoYPorElSiguienteTokenDeRefresco() {
		String email = emailUnico();
		String refresco = refrescoDe(registrar(email, "EMPRESA"));

		MvcTestResult respuesta = refrescar(refresco);

		assertThat(respuesta).hasStatusOk();
		assertThat(respuesta).bodyJson().extractingPath("$.expiresIn").isEqualTo(900);
		String acceso = leer(respuesta, "$.accessToken");
		assertThat(jwtDecoder.decode(acceso).getClaimAsString("email")).isEqualTo(email);
		assertThat(get("/api/usuarios/me", acceso)).hasStatusOk();

		String siguiente = refrescoDe(respuesta);
		assertThat(siguiente).isNotEqualTo(refresco);
		assertThat(refrescar(siguiente)).hasStatusOk();
	}

	@Test
	void dosPestanasQueRenuevanALaVezConLaMismaCookieRecibenAcceso() {
		String refresco = refrescoDe(registrar(emailUnico(), "CANDIDATO"));
		MvcTestResult primera = refrescar(refresco);

		MvcTestResult segunda = refrescar(refresco);

		assertThat(segunda).hasStatusOk();
		assertThat(get("/api/usuarios/me", leer(segunda, "$.accessToken"))).hasStatusOk();
		// No se rota otra vez: el navegador ya tiene la cookie que entregó la primera renovación
		assertThat(segunda.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
		assertThat(refrescar(refrescoDe(primera))).hasStatusOk();
	}

	@Test
	void reutilizarUnTokenYaCanjeadoCierraTodaLaSesion() {
		String email = emailUnico();
		String robado = refrescoDe(registrar(email, "CANDIDATO"));
		String vigente = refrescoDe(refrescar(robado));
		// Pasa el margen que se concede a las pestañas que renuevan a la vez
		jdbc.update("""
				update tokens_de_refresco set fecha_uso = now() - interval '1 minute'
				where fecha_uso is not null and usuario_id = (select id from usuarios where email = ?)""", email);

		MvcTestResult reutilizacion = refrescar(robado);

		assertThat(reutilizacion).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(reutilizacion).bodyJson().extractingPath("$.title").isEqualTo("Sesión no válida");
		// Ni el ladrón ni el usuario pueden seguir: hay que volver a entrar con la contraseña
		assertThat(refrescar(vigente)).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(login(email)).hasStatusOk();
	}

	@Test
	void sinCookieOConUnTokenInventadoDevuelve401YBorraLaCookie() {
		assertThat(post("/api/auth/refresco", null, "")).hasStatus(HttpStatus.UNAUTHORIZED);

		MvcTestResult inventado = refrescar("token-inventado");

		assertThat(inventado).hasStatus(HttpStatus.UNAUTHORIZED);
		Cookie cookie = inventado.getResponse().getCookie(COOKIE);
		assertThat(cookie.getValue()).isEmpty();
		assertThat(cookie.getMaxAge()).isZero();
	}

	@Test
	void unTokenDeRefrescoCaducadoNoSirve() {
		String email = emailUnico();
		String refresco = refrescoDe(registrar(email, "CANDIDATO"));
		jdbc.update("""
				update tokens_de_refresco set fecha_caducidad = now() - interval '1 minute'
				where usuario_id = (select id from usuarios where email = ?)""", email);

		assertThat(refrescar(refresco)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void salirInvalidaElTokenDeRefrescoYBorraLaCookie() {
		String refresco = refrescoDe(registrar(emailUnico(), "CANDIDATO"));

		MvcTestResult salida = mvc.post().uri("/api/auth/salida").cookie(new Cookie(COOKIE, refresco)).exchange();

		assertThat(salida).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(salida.getResponse().getCookie(COOKIE).getMaxAge()).isZero();
		assertThat(refrescar(refresco)).hasStatus(HttpStatus.UNAUTHORIZED);
		// Salir sin sesión tampoco es un error
		assertThat(post("/api/auth/salida", null, "")).hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void cadaDispositivoTieneSuSesionYSalirDeUnoNoCierraElOtro() {
		String email = emailUnico();
		String movil = refrescoDe(registrar(email, "CANDIDATO"));
		String portatil = refrescoDe(login(email));

		mvc.post().uri("/api/auth/salida").cookie(new Cookie(COOKIE, movil)).exchange();

		assertThat(refrescar(movil)).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(refrescar(portatil)).hasStatusOk();
	}

	@Test
	void cambiarLaContrasenaCierraLasDemasSesionesYRenuevaLaDeEsteDispositivo() {
		String email = emailUnico();
		String otroDispositivo = refrescoDe(registrar(email, "CANDIDATO"));
		MvcTestResult aqui = login(email);

		MvcTestResult cambio = post("/api/usuarios/me/password", leer(aqui, "$.accessToken"), """
				{"passwordActual": "%s", "passwordNueva": "otra-contraseña-segura"}
				""".formatted(PASSWORD));

		assertThat(cambio).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(refrescar(otroDispositivo)).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(refrescar(refrescoDe(aqui))).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(refrescar(refrescoDe(cambio))).hasStatusOk();
	}

	@Test
	void restablecerLaContrasenaConElEnlaceDelCorreoCierraTodasLasSesiones() {
		String email = emailUnico();
		String refresco = refrescoDe(registrar(email, "CANDIDATO"));
		post("/api/auth/recuperacion", null, """
				{"email": "%s"}
				""".formatted(email));

		assertThat(post("/api/auth/restablecimiento", null, """
				{"token": "%s", "password": "otra-contraseña-segura"}
				""".formatted(buzon.tokenDelUltimoEnlacePara(email)))).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(refrescar(refresco)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private MvcTestResult login(String email) {
		MvcTestResult respuesta = post("/api/auth/login", null, """
				{"email": "%s", "password": "%s"}
				""".formatted(email, PASSWORD));
		assertThat(respuesta).hasStatusOk();
		return respuesta;
	}

	private MvcTestResult refrescar(String tokenDeRefresco) {
		return mvc.post().uri("/api/auth/refresco").cookie(new Cookie(COOKIE, tokenDeRefresco)).exchange();
	}

	private static String refrescoDe(MvcTestResult respuesta) {
		Cookie cookie = respuesta.getResponse().getCookie(COOKIE);
		assertThat(cookie).as("La respuesta debería traer la cookie de sesión").isNotNull();
		return cookie.getValue();
	}

}

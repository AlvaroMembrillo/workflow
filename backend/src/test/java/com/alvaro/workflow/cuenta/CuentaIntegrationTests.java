package com.alvaro.workflow.cuenta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.alvaro.workflow.IntegrationTestBase;
import com.alvaro.workflow.correo.Mensaje;
import com.alvaro.workflow.usuario.Rol;
import com.alvaro.workflow.usuario.Usuario;
import com.alvaro.workflow.usuario.UsuarioRepository;

import jakarta.servlet.http.Cookie;

/** Derechos de protección de datos: copia de los datos, rectificación y borrado de la cuenta. */
class CuentaIntegrationTests extends IntegrationTestBase {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private BorradoDeCuentas borrado;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void elRegistroGuardaCuandoSeAceptaronLasCondiciones() {
		String token = nuevoCandidato();

		assertThat(get("/api/usuarios/me/datos", token)).bodyJson()
				.extractingPath("$.cuenta.condicionesAceptadasEn").isNotNull();
	}

	@Test
	void elUsuarioCorrigeSuNombre() {
		String token = nuevoCandidato();

		MvcTestResult respuesta = patch("/api/usuarios/me", token, """
				{"nombre": "  Ana García López  "}
				""");

		assertThat(respuesta).hasStatusOk();
		assertThat(respuesta).bodyJson().extractingPath("$.nombre").isEqualTo("Ana García López");
		// Lo que no se envía se queda como estaba
		assertThat(respuesta).bodyJson().extractingPath("$.avisosPorCorreo").isEqualTo(true);
		assertThat(patch("/api/usuarios/me", token, """
				{"nombre": "   "}
				""")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void elCandidatoDescargaUnaCopiaDeTodosSusDatos() {
		String empresa = nuevaEmpresa();
		String ofertaId = publicarOferta(empresa, "Backend Java");
		String email = emailUnico();
		String candidato = registrarYVerificar(email, "CANDIDATO", "Ana García");
		subirCv(candidato, "cv.pdf", pdf("currículum"));
		post("/api/ofertas/" + ofertaId + "/candidaturas", candidato, """
				{"cartaPresentacion": "Me interesa mucho"}
				""");
		post("/api/ofertas/" + ofertaId + "/denuncias", candidato, """
				{"motivo": "OTRO", "detalle": "El salario no es real"}
				""");

		MvcTestResult copia = get("/api/usuarios/me/datos", candidato);

		assertThat(copia).hasStatusOk();
		assertThat(copia.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
				.isEqualTo("attachment; filename=\"workflow-mis-datos.json\"");
		assertThat(copia.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");
		assertThat(copia).bodyJson().extractingPath("$.cuenta.email").isEqualTo(email);
		assertThat(copia).bodyJson().extractingPath("$.cuenta.nombre").isEqualTo("Ana García");
		assertThat(copia).bodyJson().extractingPath("$.cuenta.emailVerificadoEn").isNotNull();
		assertThat(copia).bodyJson().extractingPath("$.curriculum.nombreFichero").isEqualTo("cv.pdf");
		assertThat(copia).bodyJson().extractingPath("$.candidaturas[0].oferta").isEqualTo("Backend Java");
		assertThat(copia).bodyJson().extractingPath("$.candidaturas[0].empresa").isEqualTo("Empresa de prueba");
		assertThat(copia).bodyJson().extractingPath("$.candidaturas[0].cartaPresentacion").isEqualTo("Me interesa mucho");
		assertThat(copia).bodyJson().extractingPath("$.denuncias[0].detalle").isEqualTo("El salario no es real");
		assertThat(copia).bodyJson().extractingPath("$.empresa").isNull();
		assertThat(copia).bodyJson().extractingPath("$.ofertas").asArray().isEmpty();
		// Nunca sale la contraseña, ni siquiera cifrada
		assertThat(copia.getResponse().getContentAsByteArray()).asString().doesNotContain("bcrypt").doesNotContain("password");
	}

	@Test
	void laEmpresaDescargaSuPerfilYSusOfertas() {
		String empresa = nuevaEmpresa();
		publicarOferta(empresa, "Backend Java");

		MvcTestResult copia = get("/api/usuarios/me/datos", empresa);

		assertThat(copia).bodyJson().extractingPath("$.empresa.nombre").isEqualTo("Empresa de prueba");
		assertThat(copia).bodyJson().extractingPath("$.ofertas[0].titulo").isEqualTo("Backend Java");
		assertThat(copia).bodyJson().extractingPath("$.ofertas[0].salarioMinimo").isEqualTo(30000);
		assertThat(copia).bodyJson().extractingPath("$.candidaturas").asArray().isEmpty();
	}

	@Test
	void laCopiaDeDatosExigeSesion() {
		assertThat(get("/api/usuarios/me/datos", null)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void paraBorrarLaCuentaHayQueConfirmarConLaContrasena() {
		String email = emailUnico();
		String token = tokenDeRegistro(registrar(email, "CANDIDATO"));

		MvcTestResult respuesta = baja(token, "no-es-esta");

		assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(respuesta).bodyJson().extractingPath("$.errores.password").isEqualTo("La contraseña no es correcta");
		assertThat(login(email)).hasStatusOk();
	}

	@Test
	void borrarLaCuentaDeUnCandidatoEliminaSusDatosYCierraSuSesion() {
		String empresa = nuevaEmpresa();
		String ofertaId = publicarOferta(empresa, "Backend Java");
		String email = emailUnico();
		MvcTestResult registro = registrar(email, "CANDIDATO");
		String candidato = tokenDeRegistro(registro);
		String refresco = registro.getResponse().getCookie("workflow_refresco").getValue();
		verificar(email);
		subirCv(candidato, "cv.pdf", pdf("currículum"));
		post("/api/ofertas/" + ofertaId + "/candidaturas", candidato, "{}");
		post("/api/ofertas/" + ofertaId + "/denuncias", candidato, """
				{"motivo": "OTRO", "detalle": null}
				""");

		MvcTestResult respuesta = baja(candidato, PASSWORD);

		assertThat(respuesta).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(respuesta.getResponse().getCookie("workflow_refresco").getMaxAge()).isZero();
		assertThat(login(email)).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.post().uri("/api/auth/refresco").cookie(new Cookie("workflow_refresco", refresco)).exchange())
				.hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(contar("select count(*) from usuarios where email = ?", email)).isZero();
		assertThat(get("/api/ofertas/" + ofertaId + "/candidaturas", empresa)).bodyJson()
				.extractingPath("$.page.totalElements").isEqualTo(0);
		// La denuncia se conserva, pero ya no se sabe de quién era
		assertThat(contar("select count(*) from denuncias where oferta_id = ?::uuid and denunciante_id is null", ofertaId))
				.isEqualTo(1);
		Mensaje despedida = buzon.ultimoPara(email);
		assertThat(despedida.asunto()).isEqualTo("Hemos borrado tu cuenta de Workflow");
		// El email queda libre para volver a registrarse
		assertThat(registrar(email, "CANDIDATO")).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void borrarLaCuentaDeUnaEmpresaEliminaSusOfertasYAvisaAQuienEsperabaRespuesta() {
		String emailEmpresa = emailUnico();
		String empresa = registrarYVerificar(emailEmpresa, "EMPRESA", "Empresa que se va");
		String empresaId = leer(get("/api/empresas/me", empresa), "$.id");
		String ofertaId = publicarOferta(empresa, "Backend Java");
		String emailEnEspera = emailUnico();
		String enEspera = registrarYVerificar(emailEnEspera, "CANDIDATO", "Luis Ortega");
		post("/api/ofertas/" + ofertaId + "/candidaturas", enEspera, "{}");
		String emailRechazado = emailUnico();
		String rechazado = registrarYVerificar(emailRechazado, "CANDIDATO", "Marta Sanz");
		String candidaturaRechazada = leer(post("/api/ofertas/" + ofertaId + "/candidaturas", rechazado, "{}"), "$.id");
		patch("/api/candidaturas/" + candidaturaRechazada + "/estado", empresa, """
				{"estado": "RECHAZADA"}
				""");
		post("/api/ofertas/" + ofertaId + "/denuncias", rechazado, """
				{"motivo": "OTRO", "detalle": null}
				""");
		int correosDelRechazado = buzon.para(emailRechazado).size();

		assertThat(baja(empresa, PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(get("/api/ofertas/" + ofertaId, null)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(get("/api/empresas/" + empresaId, null)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(get("/api/candidaturas/me", enEspera)).bodyJson().extractingPath("$.page.totalElements").isEqualTo(0);
		assertThat(contar("select count(*) from denuncias where oferta_id = ?::uuid", ofertaId)).isZero();
		Mensaje aviso = buzon.ultimoPara(emailEnEspera);
		assertThat(aviso.asunto()).isEqualTo("Tu candidatura a Backend Java ya no está disponible");
		assertThat(aviso.texto()).contains("Empresa que se va ha cerrado su cuenta");
		// A quien ya tenía respuesta no se le escribe
		assertThat(buzon.para(emailRechazado)).hasSize(correosDelRechazado);
		assertThat(buzon.ultimoPara(emailEmpresa).asunto()).isEqualTo("Hemos borrado tu cuenta de Workflow");
	}

	@Test
	void unaCuentaDeAdministracionNoSeBorraDesdeLaWeb() {
		String email = emailUnico();
		Usuario administrador = new Usuario(email, passwordEncoder.encode(PASSWORD), "Moderación", Rol.ADMIN);
		administrador.verificarEmail();
		usuarios.save(administrador);
		String token = leer(login(email), "$.accessToken");

		assertThat(baja(token, PASSWORD)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(login(email)).hasStatusOk();
	}

	@Test
	void lasCuentasQueNoConfirmanSuEmailEn30DiasSeBorranSolas() {
		String olvidada = emailUnico();
		registrar(olvidada, "EMPRESA", "Empresa olvidada");
		String reciente = emailUnico();
		registrar(reciente, "CANDIDATO");
		String confirmada = emailUnico();
		registrarYVerificar(confirmada, "CANDIDATO", "Ana García");
		jdbc.update("update usuarios set fecha_creacion = now() - interval '31 days' where email in (?, ?)",
				olvidada, confirmada);

		int borradas = borrado.borrarLasQueNoSeVerificaron();

		assertThat(borradas).isEqualTo(1);
		assertThat(contar("select count(*) from usuarios where email = ?", olvidada)).isZero();
		assertThat(contar("select count(*) from empresas where nombre = 'Empresa olvidada'")).isZero();
		assertThat(contar("select count(*) from usuarios where email in (?, ?)", reciente, confirmada)).isEqualTo(2);
	}

	@Test
	void laIdentidadDelTitularEsPublicaParaLasPaginasLegales() {
		MvcTestResult respuesta = get("/api/legal", null);

		assertThat(respuesta).hasStatusOk();
		assertThat(respuesta).bodyJson().extractingPath("$.contacto").isEqualTo("soporte@workflow.localhost");
		assertThat(respuesta).bodyJson().extractingPath("$").asMap().containsKeys("titular", "nif", "domicilio");
	}

	private MvcTestResult baja(String token, String password) {
		return post("/api/usuarios/me/baja", token, """
				{"password": "%s"}
				""".formatted(password));
	}

	private MvcTestResult login(String email) {
		return post("/api/auth/login", null, """
				{"email": "%s", "password": "%s"}
				""".formatted(email, PASSWORD));
	}

	private void verificar(String email) {
		assertThat(post("/api/auth/verificacion", null, """
				{"token": "%s"}
				""".formatted(buzon.tokenDelUltimoEnlacePara(email)))).hasStatus(HttpStatus.NO_CONTENT);
	}

	private String publicarOferta(String tokenEmpresa, String titulo) {
		MvcTestResult respuesta = post("/api/ofertas", tokenEmpresa, """
				{"titulo": "%s", "descripcion": "Descripción de la oferta", "ubicacion": "Madrid",
				 "modalidad": "REMOTO", "tipoContrato": "INDEFINIDO", "salarioMinimo": 30000, "salarioMaximo": 40000}
				""".formatted(titulo));
		assertThat(respuesta).hasStatus(HttpStatus.CREATED);
		return leer(respuesta, "$.id");
	}

	private long contar(String sql, Object... parametros) {
		return jdbc.queryForObject(sql, Long.class, parametros);
	}

}

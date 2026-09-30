package com.alvaro.workflow.moderacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.alvaro.workflow.IntegrationTestBase;
import com.alvaro.workflow.correo.Mensaje;
import com.alvaro.workflow.usuario.Rol;
import com.alvaro.workflow.usuario.Usuario;
import com.alvaro.workflow.usuario.UsuarioRepository;

import jakarta.servlet.http.Cookie;

/** Denuncias de ofertas y lo que un administrador puede hacer con ellas. */
class ModeracionIntegrationTests extends IntegrationTestBase {

	private static final String CONTACTO = "soporte@workflow.localhost";

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void cualquierUsuarioConSesionDenunciaUnaOfertaYElEquipoRecibeUnCorreo() {
		String ofertaId = publicarOferta(nuevaEmpresa(), "Gana 3000 € desde casa");
		int antes = buzon.para(CONTACTO).size();

		MvcTestResult respuesta = denunciar(nuevoCandidato(), ofertaId, "FRAUDE", "Piden 200 € para el material");

		assertThat(respuesta).hasStatus(HttpStatus.CREATED);
		assertThat(buzon.para(CONTACTO)).hasSize(antes + 1);
		Mensaje aviso = buzon.ultimoPara(CONTACTO);
		assertThat(aviso.asunto()).isEqualTo("Denuncia de la oferta Gana 3000 € desde casa");
		assertThat(aviso.texto()).contains("Motivo: FRAUDE").contains("Piden 200 € para el material")
				.contains("http://localhost:4200/admin");
	}

	@Test
	void noSePuedeDenunciarDosVecesLaMismaOfertaNiLaPropia() {
		String empresa = nuevaEmpresa();
		String ofertaId = publicarOferta(empresa, "Oferta normal");
		String candidato = nuevoCandidato();
		denunciar(candidato, ofertaId, "OTRO", null);

		MvcTestResult repetida = denunciar(candidato, ofertaId, "FRAUDE", null);
		assertThat(repetida).hasStatus(HttpStatus.CONFLICT);
		assertThat(repetida).bodyJson().extractingPath("$.title").isEqualTo("Denuncia repetida");

		assertThat(denunciar(empresa, ofertaId, "OTRO", null)).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void denunciarExigeSesionUnMotivoValidoYUnaOfertaQueExista() {
		String ofertaId = publicarOferta(nuevaEmpresa(), "Oferta normal");
		String candidato = nuevoCandidato();

		assertThat(denunciar(null, ofertaId, "FRAUDE", null)).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(post("/api/ofertas/" + ofertaId + "/denuncias", candidato, "{}")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(denunciar(candidato, "00000000-0000-0000-0000-000000000000", "FRAUDE", null))
				.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void soloLosAdministradoresVenElPanelDeModeracion() {
		assertThat(get("/api/admin/denuncias", nuevoCandidato())).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(get("/api/admin/denuncias", nuevaEmpresa())).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(get("/api/admin/denuncias", null)).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(get("/api/admin/denuncias", nuevoAdministrador())).hasStatusOk();
	}

	@Test
	void elAdministradorVeLaDenunciaConLaOfertaLaEmpresaYQuienDenuncia() {
		String marca = marcaUnica();
		String emailEmpresa = emailUnico();
		String ofertaId = publicarOferta(registrarYVerificar(emailEmpresa, "EMPRESA", "Empresa " + marca), "Oferta " + marca);
		String emailCandidato = emailUnico();
		denunciar(registrarYVerificar(emailCandidato, "CANDIDATO", "Ana García"), ofertaId, "ENGANOSA", "El salario real es menor");

		MvcTestResult pendientes = get("/api/admin/denuncias?size=50&sort=fechaCreacion,desc", nuevoAdministrador());

		String denuncia = "$.content[?(@.oferta.id == '%s')]".formatted(ofertaId);
		assertThat(pendientes).bodyJson().extractingPath(denuncia + ".motivo").asArray().containsExactly("ENGANOSA");
		assertThat(pendientes).bodyJson().extractingPath(denuncia + ".detalle").asArray().containsExactly("El salario real es menor");
		assertThat(pendientes).bodyJson().extractingPath(denuncia + ".emailDenunciante").asArray().containsExactly(emailCandidato);
		assertThat(pendientes).bodyJson().extractingPath(denuncia + ".oferta.empresaNombre").asArray().containsExactly("Empresa " + marca);
		assertThat(pendientes).bodyJson().extractingPath(denuncia + ".oferta.empresaEmail").asArray().containsExactly(emailEmpresa);
		assertThat(pendientes).bodyJson().extractingPath(denuncia + ".oferta.descripcion").asArray().containsExactly("Descripción de la oferta");
	}

	@Test
	void retirarLaOfertaLaOcultaAlPublicoResuelveSusDenunciasYAvisaALaEmpresa() {
		String emailEmpresa = emailUnico();
		String empresa = registrarYVerificar(emailEmpresa, "EMPRESA", "Empresa de prueba");
		String titulo = "Oferta " + marcaUnica();
		String ofertaId = publicarOferta(empresa, titulo);
		String admin = nuevoAdministrador();
		denunciar(nuevoCandidato(), ofertaId, "FRAUDE", null);
		denunciar(nuevoCandidato(), ofertaId, "ENGANOSA", null);
		String denunciaId = denunciaDe(admin, ofertaId);

		MvcTestResult resolucion = resolver(admin, denunciaId, "RETIRAR_OFERTA");

		assertThat(resolucion).hasStatusOk();
		assertThat(resolucion).bodyJson().extractingPath("$.estado").isEqualTo("ACEPTADA");
		assertThat(resolucion).bodyJson().extractingPath("$.oferta.estado").isEqualTo("RETIRADA");
		// Para el público ha dejado de existir
		assertThat(get("/api/ofertas/" + ofertaId, null)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(get("/api/ofertas?texto=" + titulo, null)).bodyJson().extractingPath("$.page.totalElements").isEqualTo(0);
		assertThat(post("/api/ofertas/" + ofertaId + "/candidaturas", nuevoCandidato(), "{}")).hasStatus(HttpStatus.CONFLICT);
		// La otra denuncia de la misma oferta ya no está pendiente
		assertThat(get("/api/admin/denuncias?size=50", admin)).bodyJson()
				.extractingPath("$.content[?(@.oferta.id == '%s')]".formatted(ofertaId)).asArray().isEmpty();
		assertThat(get("/api/admin/denuncias?estado=ACEPTADA&size=50", admin)).bodyJson()
				.extractingPath("$.content[?(@.oferta.id == '%s')]".formatted(ofertaId)).asArray().hasSize(2);

		Mensaje aviso = buzon.ultimoPara(emailEmpresa);
		assertThat(aviso.asunto()).isEqualTo("Hemos retirado tu oferta " + titulo);
		assertThat(aviso.texto()).contains("incumple las normas de publicación").contains(CONTACTO);
	}

	@Test
	void laEmpresaVeSuOfertaRetiradaPeroNoPuedeEditarlaNiReabrirla() {
		String empresa = nuevaEmpresa();
		String ofertaId = publicarOferta(empresa, "Oferta que se retira");
		String admin = nuevoAdministrador();
		denunciar(nuevoCandidato(), ofertaId, "FRAUDE", null);
		resolver(admin, denunciaDe(admin, ofertaId), "RETIRAR_OFERTA");

		assertThat(get("/api/empresas/me/ofertas/" + ofertaId, empresa)).bodyJson()
				.extractingPath("$.oferta.estado").isEqualTo("RETIRADA");
		MvcTestResult reapertura = patch("/api/ofertas/" + ofertaId + "/estado", empresa, """
				{"estado": "ABIERTA"}
				""");
		assertThat(reapertura).hasStatus(HttpStatus.CONFLICT);
		assertThat(reapertura).bodyJson().extractingPath("$.title").isEqualTo("Oferta retirada");
		assertThat(put("/api/ofertas/" + ofertaId, empresa, oferta("Otro título"))).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void unaEmpresaNoPuedeRetirarSuPropiaOfertaComoSiFueraModeracion() {
		String empresa = nuevaEmpresa();
		String ofertaId = publicarOferta(empresa, "Oferta normal");

		assertThat(patch("/api/ofertas/" + ofertaId + "/estado", empresa, """
				{"estado": "RETIRADA"}
				""")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void desestimarLaDenunciaDejaLaOfertaComoEstaba() {
		String ofertaId = publicarOferta(nuevaEmpresa(), "Oferta correcta");
		String admin = nuevoAdministrador();
		denunciar(nuevoCandidato(), ofertaId, "OTRO", "No me gusta");
		String denunciaId = denunciaDe(admin, ofertaId);

		MvcTestResult resolucion = resolver(admin, denunciaId, "DESESTIMAR");

		assertThat(resolucion).bodyJson().extractingPath("$.estado").isEqualTo("DESESTIMADA");
		assertThat(get("/api/ofertas/" + ofertaId, null)).bodyJson().extractingPath("$.estado").isEqualTo("ABIERTA");
		// Una denuncia resuelta no se puede volver a resolver
		assertThat(resolver(admin, denunciaId, "RETIRAR_OFERTA")).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void suspenderUnaEmpresaRetiraSusOfertasCierraSusSesionesYNoLeDejaEntrar() {
		String email = emailUnico();
		MvcTestResult registro = registrar(email, "EMPRESA", "Empresa fraudulenta");
		String empresa = tokenDeRegistro(registro);
		String refresco = registro.getResponse().getCookie("workflow_refresco").getValue();
		post("/api/auth/verificacion", null, """
				{"token": "%s"}
				""".formatted(buzon.tokenDelUltimoEnlacePara(email)));
		String abierta = publicarOferta(empresa, "Oferta abierta");
		String cerrada = publicarOferta(empresa, "Oferta cerrada");
		patch("/api/ofertas/" + cerrada + "/estado", empresa, """
				{"estado": "CERRADA"}
				""");
		String admin = nuevoAdministrador();
		denunciar(nuevoCandidato(), abierta, "FRAUDE", null);
		String empresaId = leer(get("/api/empresas/me", empresa), "$.id");

		assertThat(post("/api/admin/empresas/" + empresaId + "/suspension", admin, "")).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(get("/api/ofertas/" + abierta, null)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(get("/api/ofertas/" + cerrada, null)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(get("/api/admin/denuncias?size=50", admin)).bodyJson()
				.extractingPath("$.content[?(@.oferta.id == '%s')]".formatted(abierta)).asArray().isEmpty();
		// Con la contraseña correcta se explica el motivo; con una incorrecta no se da ninguna pista
		MvcTestResult login = login(email, PASSWORD);
		assertThat(login).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(login).bodyJson().extractingPath("$.title").isEqualTo("Cuenta suspendida");
		assertThat(login).bodyJson().extractingPath("$.detail").asString().contains(CONTACTO);
		assertThat(login(email, "no-es-esta")).hasStatus(HttpStatus.UNAUTHORIZED);
		// La sesión que tenía abierta ya no se puede renovar, y con el token que le queda no puede publicar
		assertThat(mvc.post().uri("/api/auth/refresco").cookie(new Cookie("workflow_refresco", refresco)).exchange())
				.hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(post("/api/ofertas", empresa, oferta("Otra oferta"))).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(buzon.ultimoPara(email).asunto()).isEqualTo("Hemos suspendido tu cuenta de Workflow");
		// Suspenderla otra vez no hace nada ni envía otro correo
		int correos = buzon.para(email).size();
		assertThat(post("/api/admin/empresas/" + empresaId + "/suspension", admin, "")).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(buzon.para(email)).hasSize(correos);
	}

	@Test
	void soloUnAdministradorPuedeResolverDenunciasYSuspenderEmpresas() {
		String empresa = nuevaEmpresa();
		String ofertaId = publicarOferta(empresa, "Oferta normal");
		String candidato = nuevoCandidato();
		denunciar(candidato, ofertaId, "OTRO", null);
		String denunciaId = denunciaDe(nuevoAdministrador(), ofertaId);
		String empresaId = leer(get("/api/empresas/me", empresa), "$.id");

		assertThat(resolver(candidato, denunciaId, "RETIRAR_OFERTA")).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(resolver(empresa, denunciaId, "DESESTIMAR")).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(post("/api/admin/empresas/" + empresaId + "/suspension", candidato, "")).hasStatus(HttpStatus.FORBIDDEN);
	}

	/** Las cuentas de administrador no se pueden crear por la API: se guarda una directamente y se inicia sesión. */
	private String nuevoAdministrador() {
		String email = emailUnico();
		Usuario administrador = new Usuario(email, passwordEncoder.encode(PASSWORD), "Moderación", Rol.ADMIN);
		administrador.verificarEmail();
		usuarios.save(administrador);
		return leer(login(email, PASSWORD), "$.accessToken");
	}

	private MvcTestResult login(String email, String password) {
		return post("/api/auth/login", null, """
				{"email": "%s", "password": "%s"}
				""".formatted(email, password));
	}

	private String publicarOferta(String tokenEmpresa, String titulo) {
		MvcTestResult respuesta = post("/api/ofertas", tokenEmpresa, oferta(titulo));
		assertThat(respuesta).hasStatus(HttpStatus.CREATED);
		return leer(respuesta, "$.id");
	}

	private static String oferta(String titulo) {
		return """
				{"titulo": "%s", "descripcion": "Descripción de la oferta", "ubicacion": "Madrid",
				 "modalidad": "REMOTO", "tipoContrato": "INDEFINIDO", "salarioMinimo": 30000, "salarioMaximo": 40000}
				""".formatted(titulo);
	}

	private MvcTestResult denunciar(String token, String ofertaId, String motivo, String detalle) {
		return post("/api/ofertas/" + ofertaId + "/denuncias", token, """
				{"motivo": "%s", "detalle": %s}
				""".formatted(motivo, detalle == null ? "null" : "\"" + detalle + "\""));
	}

	/** El id de la primera denuncia pendiente de una oferta. */
	private String denunciaDe(String tokenAdmin, String ofertaId) {
		MvcTestResult pendientes = get("/api/admin/denuncias?size=50&sort=fechaCreacion,desc", tokenAdmin);
		java.util.List<String> ids = leer(pendientes, "$.content[?(@.oferta.id == '%s')].id".formatted(ofertaId));
		assertThat(ids).isNotEmpty();
		return ids.getLast();
	}

	private MvcTestResult resolver(String token, String denunciaId, String accion) {
		return post("/api/admin/denuncias/" + denunciaId + "/resolucion", token, """
				{"accion": "%s"}
				""".formatted(accion));
	}

}

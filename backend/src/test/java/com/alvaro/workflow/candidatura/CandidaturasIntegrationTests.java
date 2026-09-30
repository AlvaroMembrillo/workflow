package com.alvaro.workflow.candidatura;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.alvaro.workflow.IntegrationTestBase;

class CandidaturasIntegrationTests extends IntegrationTestBase {

	private static final String CARTA = """
			{"cartaPresentacion": "Me encantaría formar parte del equipo"}
			""";

	@Test
	void unCandidatoSeInscribeYVeSuCandidaturaConLaOfertaYLaEmpresa() {
		String ofertaId = publicarOferta(nuevaEmpresa());
		String candidato = nuevoCandidato();

		MvcTestResult inscripcion = post(candidaturasDe(ofertaId), candidato, CARTA);
		assertThat(inscripcion).hasStatus(HttpStatus.CREATED);
		assertThat(inscripcion).bodyJson().extractingPath("$.estado").isEqualTo("PENDIENTE");

		MvcTestResult mias = get("/api/candidaturas/me", candidato);
		assertThat(mias).hasStatusOk();
		assertThat(mias).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
		assertThat(mias).bodyJson().extractingPath("$.content[0].oferta.id").isEqualTo(ofertaId);
		assertThat(mias).bodyJson().extractingPath("$.content[0].oferta.titulo").isEqualTo("Desarrollador Java");
		assertThat(mias).bodyJson().extractingPath("$.content[0].oferta.empresa.nombre").isEqualTo("Empresa de prueba");
		assertThat(mias).bodyJson().extractingPath("$.content[0].cartaPresentacion")
				.isEqualTo("Me encantaría formar parte del equipo");
	}

	@Test
	void noSePuedeInscribirDosVecesEnLaMismaOferta() {
		String ofertaId = publicarOferta(nuevaEmpresa());
		String candidato = nuevoCandidato();
		post(candidaturasDe(ofertaId), candidato, CARTA);

		MvcTestResult segunda = post(candidaturasDe(ofertaId), candidato, CARTA);

		assertThat(segunda).hasStatus(HttpStatus.CONFLICT);
		assertThat(segunda).bodyJson().extractingPath("$.title").isEqualTo("Candidatura duplicada");
	}

	@Test
	void noSePuedeInscribirEnUnaOfertaCerrada() {
		String empresa = nuevaEmpresa();
		String ofertaId = publicarOferta(empresa);
		patch("/api/ofertas/" + ofertaId + "/estado", empresa, """
				{"estado": "CERRADA"}
				""");

		MvcTestResult respuesta = post(candidaturasDe(ofertaId), nuevoCandidato(), CARTA);

		assertThat(respuesta).hasStatus(HttpStatus.CONFLICT);
		assertThat(respuesta).bodyJson().extractingPath("$.title").isEqualTo("Oferta cerrada");
	}

	@Test
	void unaEmpresaNoPuedeInscribirseEnOfertas() {
		String ofertaId = publicarOferta(nuevaEmpresa());

		assertThat(post(candidaturasDe(ofertaId), nuevaEmpresa(), CARTA)).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void inscribirseEnUnaOfertaQueNoExisteDevuelve404() {
		MvcTestResult respuesta = post(candidaturasDe(UUID.randomUUID().toString()), nuevoCandidato(), CARTA);

		assertThat(respuesta).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void laEmpresaVeLasCandidaturasDeSuOfertaConLosDatosDelCandidato() {
		String empresa = nuevaEmpresa();
		String ofertaId = publicarOferta(empresa);
		String email = emailUnico();
		String candidato = registrarYVerificar(email, "CANDIDATO", "Ana García");
		post(candidaturasDe(ofertaId), candidato, CARTA);

		MvcTestResult recibidas = get(candidaturasDe(ofertaId), empresa);

		assertThat(recibidas).hasStatusOk();
		assertThat(recibidas).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
		assertThat(recibidas).bodyJson().extractingPath("$.content[0].candidato.nombre").isEqualTo("Ana García");
		assertThat(recibidas).bodyJson().extractingPath("$.content[0].candidato.email").isEqualTo(email);
		assertThat(recibidas).bodyJson().extractingPath("$.content[0].estado").isEqualTo("PENDIENTE");
	}

	@Test
	void otraEmpresaNoPuedeVerLasCandidaturasDeUnaOferta() {
		String ofertaId = publicarOferta(nuevaEmpresa());
		post(candidaturasDe(ofertaId), nuevoCandidato(), CARTA);

		assertThat(get(candidaturasDe(ofertaId), nuevaEmpresa())).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void laEmpresaHaceAvanzarLaCandidaturaYNoPuedeCambiarUnaDecisionFinal() {
		String empresa = nuevaEmpresa();
		String candidato = nuevoCandidato();
		String candidaturaId = leer(post(candidaturasDe(publicarOferta(empresa)), candidato, CARTA), "$.id");

		MvcTestResult enRevision = cambiarEstado(candidaturaId, empresa, "EN_REVISION");
		assertThat(enRevision).hasStatusOk();
		assertThat(enRevision).bodyJson().extractingPath("$.estado").isEqualTo("EN_REVISION");

		assertThat(cambiarEstado(candidaturaId, empresa, "ACEPTADA")).hasStatusOk();

		MvcTestResult rechazar = cambiarEstado(candidaturaId, empresa, "RECHAZADA");
		assertThat(rechazar).hasStatus(HttpStatus.CONFLICT);
		assertThat(rechazar).bodyJson().extractingPath("$.title").isEqualTo("Cambio de estado no permitido");

		// El candidato ve la decisión en sus candidaturas
		assertThat(get("/api/candidaturas/me", candidato)).bodyJson()
				.extractingPath("$.content[0].estado").isEqualTo("ACEPTADA");
	}

	@Test
	void niElCandidatoNiOtraEmpresaPuedenCambiarElEstado() {
		String candidato = nuevoCandidato();
		String candidaturaId = leer(post(candidaturasDe(publicarOferta(nuevaEmpresa())), candidato, CARTA), "$.id");

		assertThat(cambiarEstado(candidaturaId, candidato, "ACEPTADA")).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(cambiarEstado(candidaturaId, nuevaEmpresa(), "ACEPTADA")).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void guardaLaFechaDeCadaPasoDeLaCandidatura() {
		String empresa = nuevaEmpresa();
		String candidaturaId = leer(post(candidaturasDe(publicarOferta(empresa)), nuevoCandidato(), CARTA), "$.id");

		MvcTestResult enRevision = cambiarEstado(candidaturaId, empresa, "EN_REVISION");
		assertThat(enRevision).bodyJson().extractingPath("$.fechaRevision").isNotNull();
		assertThat(enRevision).bodyJson().extractingPath("$.fechaResolucion").isNull();

		MvcTestResult aceptada = cambiarEstado(candidaturaId, empresa, "ACEPTADA");
		assertThat(aceptada).bodyJson().extractingPath("$.fechaRevision").isNotNull();
		assertThat(aceptada).bodyJson().extractingPath("$.fechaResolucion").isNotNull();
	}

	@Test
	void elCandidatoConsultaSiYaSeInscribioEnUnaOferta() {
		String ofertaId = publicarOferta(nuevaEmpresa());
		String candidato = nuevoCandidato();

		assertThat(get("/api/ofertas/" + ofertaId + "/mi-candidatura", candidato)).hasStatus(HttpStatus.NOT_FOUND);

		String candidaturaId = leer(post(candidaturasDe(ofertaId), candidato, CARTA), "$.id");
		MvcTestResult inscrito = get("/api/ofertas/" + ofertaId + "/mi-candidatura", candidato);
		assertThat(inscrito).hasStatusOk();
		assertThat(inscrito).bodyJson().extractingPath("$.id").isEqualTo(candidaturaId);
		assertThat(inscrito).bodyJson().extractingPath("$.estado").isEqualTo("PENDIENTE");
	}

	@Test
	void elCandidatoRetiraSuCandidaturaYLaEmpresaLoVe() {
		String empresa = nuevaEmpresa();
		String ofertaId = publicarOferta(empresa);
		String candidato = nuevoCandidato();
		String candidaturaId = leer(post(candidaturasDe(ofertaId), candidato, CARTA), "$.id");

		MvcTestResult retirada = post("/api/candidaturas/" + candidaturaId + "/retirada", candidato, "{}");

		assertThat(retirada).hasStatusOk();
		assertThat(retirada).bodyJson().extractingPath("$.estado").isEqualTo("RETIRADA");
		assertThat(retirada).bodyJson().extractingPath("$.fechaResolucion").isNotNull();
		assertThat(get(candidaturasDe(ofertaId), empresa)).bodyJson()
				.extractingPath("$.content[0].estado").isEqualTo("RETIRADA");
		// Ya no se puede decidir sobre ella
		assertThat(cambiarEstado(candidaturaId, empresa, "ACEPTADA")).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void noSePuedeRetirarUnaCandidaturaYaDecididaNiLaDeOtraPersona() {
		String empresa = nuevaEmpresa();
		String candidaturaId = leer(post(candidaturasDe(publicarOferta(empresa)), nuevoCandidato(), CARTA), "$.id");

		assertThat(post("/api/candidaturas/" + candidaturaId + "/retirada", nuevoCandidato(), "{}"))
				.hasStatus(HttpStatus.FORBIDDEN);
		assertThat(post("/api/candidaturas/" + candidaturaId + "/retirada", empresa, "{}"))
				.hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void unaCandidaturaAceptadaNoSePuedeRetirar() {
		String empresa = nuevaEmpresa();
		String candidato = nuevoCandidato();
		String candidaturaId = leer(post(candidaturasDe(publicarOferta(empresa)), candidato, CARTA), "$.id");
		cambiarEstado(candidaturaId, empresa, "ACEPTADA");

		MvcTestResult respuesta = post("/api/candidaturas/" + candidaturaId + "/retirada", candidato, "{}");

		assertThat(respuesta).hasStatus(HttpStatus.CONFLICT);
		assertThat(respuesta).bodyJson().extractingPath("$.title").isEqualTo("Cambio de estado no permitido");
	}

	@Test
	void laEmpresaNoPuedeMarcarUnaCandidaturaComoRetirada() {
		String empresa = nuevaEmpresa();
		String candidaturaId = leer(post(candidaturasDe(publicarOferta(empresa)), nuevoCandidato(), CARTA), "$.id");

		MvcTestResult respuesta = cambiarEstado(candidaturaId, empresa, "RETIRADA");

		assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(respuesta).bodyJson().extractingPath("$.title").isEqualTo("Estado no permitido");
	}

	@Test
	void laEmpresaFiltraLasCandidaturasRecibidasPorEstado() {
		String empresa = nuevaEmpresa();
		String ofertaId = publicarOferta(empresa);
		String enRevisionId = leer(post(candidaturasDe(ofertaId), nuevoCandidato(), CARTA), "$.id");
		post(candidaturasDe(ofertaId), nuevoCandidato(), CARTA);
		post(candidaturasDe(ofertaId), nuevoCandidato(), CARTA);
		cambiarEstado(enRevisionId, empresa, "EN_REVISION");

		MvcTestResult pendientes = get(candidaturasDe(ofertaId) + "?estado=PENDIENTE", empresa);
		assertThat(pendientes).bodyJson().extractingPath("$.page.totalElements").isEqualTo(2);

		MvcTestResult abiertas = get(candidaturasDe(ofertaId) + "?estado=PENDIENTE&estado=EN_REVISION", empresa);
		assertThat(abiertas).bodyJson().extractingPath("$.page.totalElements").isEqualTo(3);

		assertThat(get(candidaturasDe(ofertaId) + "?estado=INVENTADO", empresa)).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void haceFaltaElEmailConfirmadoParaInscribirse() {
		String ofertaId = publicarOferta(nuevaEmpresa());
		String email = emailUnico();
		String candidato = tokenDeRegistro(registrar(email, "CANDIDATO"));

		MvcTestResult sinConfirmar = post(candidaturasDe(ofertaId), candidato, "{}");

		assertThat(sinConfirmar).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(sinConfirmar).bodyJson().extractingPath("$.title").isEqualTo("Email sin confirmar");
		assertThat(sinConfirmar).bodyJson().extractingPath("$.detail").asString()
				.startsWith("Confirma tu email para inscribirte en ofertas.");

		post("/api/auth/verificacion", null, """
				{"token": "%s"}
				""".formatted(buzon.tokenDelUltimoEnlacePara(email)));
		assertThat(post(candidaturasDe(ofertaId), candidato, "{}")).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void laEmpresaVeQueElCandidatoTieneCurriculumYLoDescarga() {
		String empresa = nuevaEmpresa();
		String ofertaId = publicarOferta(empresa);
		String conCv = nuevoCandidato();
		subirCv(conCv, "cv.pdf", pdf("currículum"));
		String candidaturaConCv = leer(post(candidaturasDe(ofertaId), conCv, "{}"), "$.id");
		String candidaturaSinCv = leer(post(candidaturasDe(ofertaId), nuevoCandidato(), "{}"), "$.id");

		MvcTestResult recibidas = get(candidaturasDe(ofertaId) + "?sort=fechaCreacion,asc", empresa);

		assertThat(recibidas).bodyJson().extractingPath("$.content[0].cv.nombreFichero").isEqualTo("cv.pdf");
		assertThat(recibidas).bodyJson().extractingPath("$.content[1].cv").isNull();

		MvcTestResult descarga = get("/api/candidaturas/" + candidaturaConCv + "/cv", empresa);
		assertThat(descarga).hasStatusOk().hasContentType(MediaType.APPLICATION_PDF);
		assertThat(descarga.getResponse().getContentAsByteArray()).isEqualTo(pdf("currículum"));
		assertThat(get("/api/candidaturas/" + candidaturaSinCv + "/cv", empresa)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void elCurriculumSoloLoDescargaLaEmpresaDeLaOfertaYMientrasLaCandidaturaSigaEnPie() {
		String empresa = nuevaEmpresa();
		String candidato = nuevoCandidato();
		subirCv(candidato, "cv.pdf", pdf("currículum"));
		String candidaturaId = leer(post(candidaturasDe(publicarOferta(empresa)), candidato, "{}"), "$.id");
		String cv = "/api/candidaturas/" + candidaturaId + "/cv";

		assertThat(get(cv, nuevaEmpresa())).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(get(cv, candidato)).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(get(cv, null)).hasStatus(HttpStatus.UNAUTHORIZED);

		post("/api/candidaturas/" + candidaturaId + "/retirada", candidato, "");

		assertThat(get(cv, empresa)).hasStatus(HttpStatus.NOT_FOUND);
	}

	private String publicarOferta(String tokenEmpresa) {
		MvcTestResult respuesta = post("/api/ofertas", tokenEmpresa, """
				{"titulo": "Desarrollador Java", "descripcion": "Buscamos a alguien con ganas de aprender",
				 "ubicacion": "Madrid", "modalidad": "REMOTO", "tipoContrato": "INDEFINIDO",
				 "salarioMinimo": 30000, "salarioMaximo": 40000}
				""");
		assertThat(respuesta).hasStatus(HttpStatus.CREATED);
		return leer(respuesta, "$.id");
	}

	private MvcTestResult cambiarEstado(String candidaturaId, String token, String estado) {
		return patch("/api/candidaturas/" + candidaturaId + "/estado", token, """
				{"estado": "%s"}
				""".formatted(estado));
	}

	private static String candidaturasDe(String ofertaId) {
		return "/api/ofertas/" + ofertaId + "/candidaturas";
	}

}

package com.alvaro.workflow.candidatura;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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
		String candidato = leer(registrar(email, "CANDIDATO"), "$.accessToken");
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

	private String publicarOferta(String tokenEmpresa) {
		MvcTestResult respuesta = post("/api/ofertas", tokenEmpresa, """
				{"titulo": "Desarrollador Java", "descripcion": "Buscamos a alguien con ganas de aprender",
				 "ubicacion": "Madrid", "modalidad": "REMOTO", "tipoContrato": "INDEFINIDO"}
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

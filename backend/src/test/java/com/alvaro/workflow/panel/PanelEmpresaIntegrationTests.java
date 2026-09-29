package com.alvaro.workflow.panel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.alvaro.workflow.IntegrationTestBase;

class PanelEmpresaIntegrationTests extends IntegrationTestBase {

	@Test
	void cadaOfertaIndicaCuantasCandidaturasTieneEnCadaEstado() {
		String empresa = nuevaEmpresa();
		String conCandidaturas = publicarOferta(empresa, "Backend Java");
		String sinCandidaturas = publicarOferta(empresa, "Frontend Angular");
		inscribir(conCandidaturas, nuevoCandidato());
		String aceptada = inscribir(conCandidaturas, nuevoCandidato());
		String enRevision = inscribir(conCandidaturas, nuevoCandidato());
		String candidatoQueRetira = nuevoCandidato();
		String retirada = inscribir(conCandidaturas, candidatoQueRetira);
		patch("/api/candidaturas/" + aceptada + "/estado", empresa, """
				{"estado": "ACEPTADA"}
				""");
		patch("/api/candidaturas/" + enRevision + "/estado", empresa, """
				{"estado": "EN_REVISION"}
				""");
		post("/api/candidaturas/" + retirada + "/retirada", candidatoQueRetira, "{}");

		MvcTestResult panel = get("/api/empresas/me/ofertas?sort=fechaCreacion,asc", empresa);

		assertThat(panel).hasStatusOk();
		assertThat(panel).bodyJson().extractingPath("$.content[0].oferta.id").isEqualTo(conCandidaturas);
		assertThat(panel).bodyJson().extractingPath("$.content[0].candidaturas").asMap()
				.containsEntry("total", 4)
				.containsEntry("pendientes", 1)
				.containsEntry("enRevision", 1)
				.containsEntry("aceptadas", 1)
				.containsEntry("rechazadas", 0)
				.containsEntry("retiradas", 1);
		assertThat(panel).bodyJson().extractingPath("$.content[1].oferta.id").isEqualTo(sinCandidaturas);
		assertThat(panel).bodyJson().extractingPath("$.content[1].candidaturas.total").isEqualTo(0);
	}

	@Test
	void soloLasEmpresasTienenPanel() {
		assertThat(get("/api/empresas/me/ofertas", nuevoCandidato())).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(get("/api/empresas/me/ofertas", null)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private String publicarOferta(String tokenEmpresa, String titulo) {
		return leer(post("/api/ofertas", tokenEmpresa, """
				{"titulo": "%s", "descripcion": "Descripción", "ubicacion": "Madrid", "modalidad": "REMOTO",
				 "tipoContrato": "INDEFINIDO", "salarioMinimo": 30000, "salarioMaximo": 40000}
				""".formatted(titulo)), "$.id");
	}

	private String inscribir(String ofertaId, String tokenCandidato) {
		return leer(post("/api/ofertas/" + ofertaId + "/candidaturas", tokenCandidato, "{}"), "$.id");
	}

}

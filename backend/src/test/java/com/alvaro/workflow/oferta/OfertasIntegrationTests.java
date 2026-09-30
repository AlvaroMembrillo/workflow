package com.alvaro.workflow.oferta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.alvaro.workflow.IntegrationTestBase;

class OfertasIntegrationTests extends IntegrationTestBase {

	@Test
	void unaEmpresaPublicaUnaOfertaYCualquieraPuedeVerla() {
		MvcTestResult publicada = post("/api/ofertas", nuevaEmpresa(), oferta("Desarrollador Java", "REMOTO"));

		assertThat(publicada).hasStatus(HttpStatus.CREATED);
		String id = leer(publicada, "$.id");
		assertThat(publicada.getResponse().getHeader(HttpHeaders.LOCATION)).endsWith("/api/ofertas/" + id);

		MvcTestResult consultada = get("/api/ofertas/" + id, null);
		assertThat(consultada).hasStatusOk();
		assertThat(consultada).bodyJson().extractingPath("$.titulo").isEqualTo("Desarrollador Java");
		assertThat(consultada).bodyJson().extractingPath("$.estado").isEqualTo("ABIERTA");
		assertThat(consultada).bodyJson().extractingPath("$.empresa.nombre").isEqualTo("Empresa de prueba");
	}

	@Test
	void haceFaltaElEmailConfirmadoParaPublicar() {
		String email = emailUnico();
		String empresa = tokenDeRegistro(registrar(email, "EMPRESA", "Empresa sin confirmar"));

		MvcTestResult sinConfirmar = post("/api/ofertas", empresa, oferta("Backend Java", "REMOTO"));

		assertThat(sinConfirmar).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(sinConfirmar).bodyJson().extractingPath("$.title").isEqualTo("Email sin confirmar");
		assertThat(sinConfirmar).bodyJson().extractingPath("$.detail").asString()
				.startsWith("Confirma tu email para publicar ofertas.");

		post("/api/auth/verificacion", null, """
				{"token": "%s"}
				""".formatted(buzon.tokenDelUltimoEnlacePara(email)));
		assertThat(post("/api/ofertas", empresa, oferta("Backend Java", "REMOTO"))).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void unCandidatoNoPuedePublicarOfertas() {
		MvcTestResult respuesta = post("/api/ofertas", nuevoCandidato(), oferta("Desarrollador Java", "REMOTO"));

		assertThat(respuesta).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void sinTokenNoSePuedePublicar() {
		assertThat(post("/api/ofertas", null, oferta("Desarrollador Java", "REMOTO"))).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void rechazaUnaOfertaSinLosCamposObligatorios() {
		MvcTestResult respuesta = post("/api/ofertas", nuevaEmpresa(), """
				{"titulo": "", "salarioMinimo": -1}
				""");

		assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(respuesta).bodyJson().extractingPath("$.errores").asMap()
				.containsOnlyKeys("titulo", "descripcion", "ubicacion", "modalidad", "tipoContrato", "salarioMinimo",
						"salarioMaximo");
	}

	@Test
	void rechazaUnSalarioMinimoMayorQueElMaximo() {
		MvcTestResult respuesta = post("/api/ofertas", nuevaEmpresa(), """
				{"titulo": "Desarrollador Java", "descripcion": "Descripción", "ubicacion": "Madrid",
				 "modalidad": "REMOTO", "tipoContrato": "INDEFINIDO", "salarioMinimo": 40000, "salarioMaximo": 30000}
				""");

		assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(respuesta).bodyJson().extractingPath("$.title").isEqualTo("Rango salarial no válido");
	}

	@Test
	void soloLaEmpresaQueLaPublicoPuedeModificarla() {
		String empresa = nuevaEmpresa();
		String id = leer(post("/api/ofertas", empresa, oferta("Desarrollador Java", "REMOTO")), "$.id");

		MvcTestResult deOtraEmpresa = put("/api/ofertas/" + id, nuevaEmpresa(), oferta("Título cambiado", "REMOTO"));
		assertThat(deOtraEmpresa).hasStatus(HttpStatus.FORBIDDEN);

		MvcTestResult deLaPropia = put("/api/ofertas/" + id, empresa, oferta("Desarrollador Java Senior", "HIBRIDO"));
		assertThat(deLaPropia).hasStatusOk();
		assertThat(deLaPropia).bodyJson().extractingPath("$.titulo").isEqualTo("Desarrollador Java Senior");
		assertThat(deLaPropia).bodyJson().extractingPath("$.modalidad").isEqualTo("HIBRIDO");
	}

	@Test
	void unaOfertaCerradaDesapareceDelListadoPublicoPeroNoDelDeLaEmpresa() {
		String empresa = nuevaEmpresa();
		String marca = marcaUnica();
		String id = leer(post("/api/ofertas", empresa, oferta("Oferta " + marca, "REMOTO")), "$.id");
		assertThat(get("/api/ofertas?texto=" + marca, null)).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);

		MvcTestResult cerrada = patch("/api/ofertas/" + id + "/estado", empresa, """
				{"estado": "CERRADA"}
				""");
		assertThat(cerrada).hasStatusOk();
		assertThat(cerrada).bodyJson().extractingPath("$.estado").isEqualTo("CERRADA");

		assertThat(get("/api/ofertas?texto=" + marca, null)).bodyJson().extractingPath("$.page.totalElements").isEqualTo(0);
		MvcTestResult misOfertas = get("/api/empresas/me/ofertas", empresa);
		assertThat(misOfertas).bodyJson().extractingPath("$.content[0].oferta.id").isEqualTo(id);
		assertThat(misOfertas).bodyJson().extractingPath("$.content[0].oferta.estado").isEqualTo("CERRADA");
	}

	@Test
	void filtraPorTextoSinDistinguirMayusculasYPorModalidadYPagina() {
		String empresa = nuevaEmpresa();
		String marca = marcaUnica();
		post("/api/ofertas", empresa, oferta(marca + " Java", "REMOTO"));
		post("/api/ofertas", empresa, oferta(marca + " Python", "REMOTO"));
		post("/api/ofertas", empresa, oferta(marca + " Java", "PRESENCIAL"));

		MvcTestResult java = get("/api/ofertas?texto=" + marca.toUpperCase() + " JAVA", null);
		assertThat(java).bodyJson().extractingPath("$.page.totalElements").isEqualTo(2);

		MvcTestResult remotasPorPaginas = get("/api/ofertas?texto=" + marca + "&modalidad=REMOTO&size=1", null);
		assertThat(remotasPorPaginas).hasStatusOk();
		assertThat(remotasPorPaginas).bodyJson().extractingPath("$.content").asArray().hasSize(1);
		assertThat(remotasPorPaginas).bodyJson().extractingPath("$.page.totalElements").isEqualTo(2);
		assertThat(remotasPorPaginas).bodyJson().extractingPath("$.page.totalPages").isEqualTo(2);
	}

	@Test
	void filtraLasOfertasDeUnaEmpresaParaSuPerfilPublico() {
		String marca = marcaUnica();
		String empresa = nuevaEmpresa();
		post("/api/ofertas", empresa, oferta(marca + " Java", "REMOTO"));
		post("/api/ofertas", nuevaEmpresa(), oferta(marca + " Java", "REMOTO"));
		String empresaId = leer(get("/api/empresas/me", empresa), "$.id");

		MvcTestResult deLaEmpresa = get("/api/ofertas?texto=" + marca + "&empresaId=" + empresaId, null);

		assertThat(deLaEmpresa).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
		assertThat(deLaEmpresa).bodyJson().extractingPath("$.content[0].empresa.id").isEqualTo(empresaId);
	}

	@Test
	void losComodinesDeSqlSeBuscanComoTextoNormal() {
		String marca = marcaUnica();
		post("/api/ofertas", nuevaEmpresa(), oferta(marca + " Java", "REMOTO"));

		// Sin escapar, "%" encajaría con cualquier texto y encontraría la oferta
		MvcTestResult respuesta = mvc.get().uri("/api/ofertas?texto={texto}", marca + "%").exchange();

		assertThat(respuesta).bodyJson().extractingPath("$.page.totalElements").isEqualTo(0);
	}

	@Test
	void ordenarPorUnCampoQueNoExisteDevuelve400() {
		MvcTestResult respuesta = get("/api/ofertas?sort=noExiste", null);

		assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(respuesta).bodyJson().extractingPath("$.title").isEqualTo("Orden no válido");
	}

	@Test
	void unaOfertaQueNoExisteDevuelve404() {
		assertThat(get("/api/ofertas/" + UUID.randomUUID(), null)).hasStatus(HttpStatus.NOT_FOUND);
	}

	private static String oferta(String titulo, String modalidad) {
		return """
				{"titulo": "%s", "descripcion": "Buscamos a alguien con ganas de aprender", "ubicacion": "Madrid",
				 "modalidad": "%s", "tipoContrato": "INDEFINIDO", "salarioMinimo": 30000, "salarioMaximo": 40000}
				""".formatted(titulo, modalidad);
	}

}

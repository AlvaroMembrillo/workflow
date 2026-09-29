package com.alvaro.workflow.empresa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.alvaro.workflow.IntegrationTestBase;

class EmpresasIntegrationTests extends IntegrationTestBase {

	@Test
	void alRegistrarUnaEmpresaSeCreaSuPerfilConElNombreDelRegistro() {
		MvcTestResult respuesta = get("/api/empresas/me", nuevaEmpresa());

		assertThat(respuesta).hasStatusOk();
		assertThat(respuesta).bodyJson().extractingPath("$.nombre").isEqualTo("Empresa de prueba");
	}

	@Test
	void laEmpresaActualizaSuPerfilYCualquieraPuedeVerlo() {
		String empresa = nuevaEmpresa();

		MvcTestResult actualizada = put("/api/empresas/me", empresa, """
				{"nombre": "Acme Software", "descripcion": "Hacemos software", "sitioWeb": "https://acme.example",
				 "ubicacion": "Sevilla"}
				""");
		assertThat(actualizada).hasStatusOk();

		MvcTestResult publica = get("/api/empresas/" + leer(actualizada, "$.id"), null);
		assertThat(publica).hasStatusOk();
		assertThat(publica).bodyJson().extractingPath("$.nombre").isEqualTo("Acme Software");
		assertThat(publica).bodyJson().extractingPath("$.sitioWeb").isEqualTo("https://acme.example");
		assertThat(publica).bodyJson().extractingPath("$.ubicacion").isEqualTo("Sevilla");
	}

	@Test
	void rechazaUnSitioWebQueNoEsUnaUrl() {
		MvcTestResult respuesta = put("/api/empresas/me", nuevaEmpresa(), """
				{"nombre": "Acme", "sitioWeb": "no es una url"}
				""");

		assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(respuesta).bodyJson().extractingPath("$.errores").asMap().containsOnlyKeys("sitioWeb");
	}

	@Test
	void unCandidatoNoPuedeGestionarUnPerfilDeEmpresa() {
		MvcTestResult respuesta = get("/api/empresas/me", nuevoCandidato());

		assertThat(respuesta).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(respuesta).bodyJson().extractingPath("$.title").isEqualTo("Acceso denegado");
	}

	@Test
	void sinTokenNoSePuedeVerElPerfilPropio() {
		assertThat(get("/api/empresas/me", null)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void unaEmpresaQueNoExisteDevuelve404() {
		assertThat(get("/api/empresas/" + UUID.randomUUID(), null)).hasStatus(HttpStatus.NOT_FOUND);
	}

}

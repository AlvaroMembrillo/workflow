package com.alvaro.workflow.candidatura;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.alvaro.workflow.IntegrationTestBase;
import com.alvaro.workflow.correo.Mensaje;

/** Correos que avisan a la empresa de una candidatura nueva y al candidato de cada cambio de estado. */
class AvisosDeCandidaturasIntegrationTests extends IntegrationTestBase {

	@Test
	void laEmpresaRecibeUnCorreoPorCadaCandidaturaNueva() {
		String emailEmpresa = emailUnico();
		String empresa = registrarYVerificar(emailEmpresa, "EMPRESA", "Empresa de prueba");
		String ofertaId = publicarOferta(empresa);

		inscribirse(nuevoCandidato(), ofertaId);

		Mensaje aviso = buzon.ultimoPara(emailEmpresa);
		assertThat(aviso.asunto()).isEqualTo("Nueva candidatura para Desarrollador/a Java");
		assertThat(aviso.texto()).contains("Ana García se ha inscrito en tu oferta \"Desarrollador/a Java\"")
				.contains("http://localhost:4200/empresa/ofertas/" + ofertaId + "/candidaturas")
				.contains("http://localhost:4200/cuenta");
	}

	@Test
	void elCandidatoRecibeUnCorreoCuandoLaEmpresaMueveSuCandidatura() {
		String empresa = nuevaEmpresa();
		String emailCandidato = emailUnico();
		String candidato = registrarYVerificar(emailCandidato, "CANDIDATO", "Luis Ortega");
		String candidaturaId = inscribirse(candidato, publicarOferta(empresa));

		cambiarEstado(candidaturaId, empresa, "EN_REVISION");

		Mensaje enRevision = buzon.ultimoPara(emailCandidato);
		assertThat(enRevision.asunto()).isEqualTo("Tu candidatura a Desarrollador/a Java está en revisión");
		assertThat(enRevision.texto()).contains("Hola, Luis Ortega")
				.contains("Empresa de prueba está revisando tu candidatura")
				.contains("http://localhost:4200/mis-candidaturas");

		cambiarEstado(candidaturaId, empresa, "ACEPTADA");

		assertThat(buzon.ultimoPara(emailCandidato).asunto())
				.isEqualTo("Tu candidatura a Desarrollador/a Java ha sido seleccionada");
	}

	@Test
	void elRechazoSeComunicaSinLaPalabraRechazada() {
		String empresa = nuevaEmpresa();
		String emailCandidato = emailUnico();
		String candidato = registrarYVerificar(emailCandidato, "CANDIDATO", "Luis Ortega");
		String candidaturaId = inscribirse(candidato, publicarOferta(empresa));

		cambiarEstado(candidaturaId, empresa, "RECHAZADA");

		Mensaje aviso = buzon.ultimoPara(emailCandidato);
		assertThat(aviso.asunto()).isEqualTo("Novedades de tu candidatura a Desarrollador/a Java");
		assertThat(aviso.texto()).contains("ha decidido no continuar con tu candidatura").doesNotContainIgnoringCase("rechaz");
	}

	@Test
	void noSeEnvianAvisosAUnEmailSinVerificar() {
		String emailEmpresa = emailUnico();
		String empresa = tokenDeRegistro(registrar(emailEmpresa, "EMPRESA", "Empresa de prueba"));
		String emailCandidato = emailUnico();
		String candidato = tokenDeRegistro(registrar(emailCandidato, "CANDIDATO"));

		String candidaturaId = inscribirse(candidato, publicarOferta(empresa));
		cambiarEstado(candidaturaId, empresa, "EN_REVISION");

		// Cada uno solo tiene el correo de verificación del registro
		assertThat(buzon.para(emailEmpresa)).extracting(Mensaje::asunto).containsExactly("Confirma tu email en Workflow");
		assertThat(buzon.para(emailCandidato)).extracting(Mensaje::asunto).containsExactly("Confirma tu email en Workflow");
	}

	@Test
	void quienDesactivaLosAvisosNoLosRecibe() {
		String empresa = nuevaEmpresa();
		String emailCandidato = emailUnico();
		String candidato = registrarYVerificar(emailCandidato, "CANDIDATO", "Luis Ortega");
		patch("/api/usuarios/me", candidato, """
				{"avisosPorCorreo": false}
				""");
		String candidaturaId = inscribirse(candidato, publicarOferta(empresa));

		cambiarEstado(candidaturaId, empresa, "EN_REVISION");

		assertThat(buzon.para(emailCandidato)).hasSize(1);
	}

	@Test
	void laEmpresaNoRecibeCorreoCuandoElCandidatoRetiraSuCandidatura() {
		String emailEmpresa = emailUnico();
		String empresa = registrarYVerificar(emailEmpresa, "EMPRESA", "Empresa de prueba");
		String candidato = nuevoCandidato();
		String candidaturaId = inscribirse(candidato, publicarOferta(empresa));
		int antes = buzon.para(emailEmpresa).size();

		assertThat(post("/api/candidaturas/" + candidaturaId + "/retirada", candidato, "")).hasStatusOk();

		assertThat(buzon.para(emailEmpresa)).hasSize(antes);
	}

	private String publicarOferta(String tokenEmpresa) {
		MvcTestResult respuesta = post("/api/ofertas", tokenEmpresa, """
				{"titulo": "Desarrollador/a Java", "descripcion": "Descripción de la oferta", "ubicacion": "Madrid",
				 "modalidad": "REMOTO", "tipoContrato": "INDEFINIDO", "salarioMinimo": 30000, "salarioMaximo": 40000}
				""");
		assertThat(respuesta).hasStatus(HttpStatus.CREATED);
		return leer(respuesta, "$.id");
	}

	private String inscribirse(String tokenCandidato, String ofertaId) {
		MvcTestResult respuesta = post("/api/ofertas/" + ofertaId + "/candidaturas", tokenCandidato, "{}");
		assertThat(respuesta).hasStatus(HttpStatus.CREATED);
		return leer(respuesta, "$.id");
	}

	private void cambiarEstado(String candidaturaId, String tokenEmpresa, String estado) {
		assertThat(patch("/api/candidaturas/" + candidaturaId + "/estado", tokenEmpresa, """
				{"estado": "%s"}
				""".formatted(estado))).hasStatusOk();
	}

}

package com.alvaro.workflow.cv;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.alvaro.workflow.IntegrationTestBase;

class CurriculumIntegrationTests extends IntegrationTestBase {

	private static final String CV = "/api/candidatos/me/cv";

	@Test
	void elCandidatoSubeSuCurriculumYLoDescarga() {
		String candidato = nuevoCandidato();
		byte[] contenido = pdf("currículum de Ana");

		MvcTestResult subida = subirCv(candidato, "CV Ana García.pdf", contenido);

		assertThat(subida).hasStatusOk();
		assertThat(subida).bodyJson().extractingPath("$.nombreFichero").isEqualTo("CV Ana García.pdf");
		assertThat(subida).bodyJson().extractingPath("$.tamano").isEqualTo(contenido.length);
		assertThat(get(CV, candidato)).bodyJson().extractingPath("$.nombreFichero").isEqualTo("CV Ana García.pdf");

		MvcTestResult descarga = get(CV + "/fichero", candidato);
		assertThat(descarga).hasStatusOk().hasContentType(MediaType.APPLICATION_PDF);
		assertThat(descarga.getResponse().getContentAsByteArray()).isEqualTo(contenido);
		// Siempre como descarga y sin cachés: es un fichero subido por un usuario, con datos personales
		assertThat(descarga.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
				.startsWith("attachment;").contains("CV%20Ana%20Garc%C3%ADa.pdf");
		assertThat(descarga.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");
		assertThat(descarga.getResponse().getHeader("Content-Security-Policy")).isEqualTo("sandbox");
	}

	@Test
	void subirOtroCurriculumSustituyeAlAnterior() {
		String candidato = nuevoCandidato();
		subirCv(candidato, "viejo.pdf", pdf("versión antigua"));

		assertThat(subirCv(candidato, "nuevo.pdf", pdf("versión nueva"))).hasStatusOk();

		assertThat(get(CV, candidato)).bodyJson().extractingPath("$.nombreFichero").isEqualTo("nuevo.pdf");
		assertThat(get(CV + "/fichero", candidato).getResponse().getContentAsByteArray()).isEqualTo(pdf("versión nueva"));
	}

	@Test
	void sinCurriculumDevuelve404() {
		String candidato = nuevoCandidato();

		assertThat(get(CV, candidato)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(get(CV + "/fichero", candidato)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void elCandidatoBorraSuCurriculum() {
		String candidato = nuevoCandidato();
		subirCv(candidato, "cv.pdf", pdf("contenido"));

		assertThat(delete(CV, candidato)).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(get(CV, candidato)).hasStatus(HttpStatus.NOT_FOUND);
		// Borrarlo otra vez no es un error
		assertThat(delete(CV, candidato)).hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void soloSeAceptanPdfAunqueElFicheroDigaSerlo() {
		String candidato = nuevoCandidato();

		MvcTestResult respuesta = subirCv(candidato, "virus.pdf", "MZ esto es un ejecutable".getBytes());

		assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(respuesta).bodyJson().extractingPath("$.errores.fichero").isEqualTo("El currículum tiene que ser un PDF");
		assertThat(get(CV, candidato)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void unFicheroVacioOMayorDe5MbDevuelve400() {
		String candidato = nuevoCandidato();

		assertThat(subirCv(candidato, "vacio.pdf", new byte[0])).hasStatus(HttpStatus.BAD_REQUEST);

		byte[] enorme = new byte[5 * 1024 * 1024 + 1];
		System.arraycopy(pdf(""), 0, enorme, 0, pdf("").length);
		MvcTestResult respuesta = subirCv(candidato, "enorme.pdf", enorme);
		assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(respuesta).bodyJson().extractingPath("$.errores.fichero")
				.isEqualTo("El currículum no puede ocupar más de 5 MB");
	}

	@Test
	void unaEmpresaNoTieneCurriculum() {
		String empresa = nuevaEmpresa();

		assertThat(subirCv(empresa, "cv.pdf", pdf("contenido"))).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(get(CV, empresa)).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void sinSesionDevuelve401() {
		assertThat(get(CV, null)).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(subirCv(null, "cv.pdf", pdf("contenido"))).hasStatus(HttpStatus.UNAUTHORIZED);
	}

}

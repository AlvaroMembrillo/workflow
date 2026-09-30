package com.alvaro.workflow.cv;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CurriculumServiceTests {

	@Test
	void conservaUnNombreNormal() {
		assertThat(CurriculumService.nombreSeguro("CV Ana García 2026.pdf")).isEqualTo("CV Ana García 2026.pdf");
	}

	@Test
	void quitaLasCarpetasQueEnvianAlgunosNavegadores() {
		assertThat(CurriculumService.nombreSeguro("C:\\Users\\ana\\Documentos\\cv.pdf")).isEqualTo("cv.pdf");
		assertThat(CurriculumService.nombreSeguro("../../etc/cv.pdf")).isEqualTo("cv.pdf");
	}

	@Test
	void quitaLosCaracteresQueRomperianLaCabeceraDeLaDescarga() {
		assertThat(CurriculumService.nombreSeguro("cv\r\nSet-Cookie: x=1\".pdf")).isEqualTo("cvSet-Cookie x=1.pdf");
	}

	@Test
	void siempreTerminaEnPdf() {
		assertThat(CurriculumService.nombreSeguro("curriculum.exe")).isEqualTo("curriculum.exe.pdf");
		assertThat(CurriculumService.nombreSeguro("CURRICULUM.PDF")).isEqualTo("CURRICULUM.PDF");
	}

	@Test
	void sinNombreUsaUnoPorDefecto() {
		assertThat(CurriculumService.nombreSeguro(null)).isEqualTo("curriculum.pdf");
		assertThat(CurriculumService.nombreSeguro("   ")).isEqualTo("curriculum.pdf");
		assertThat(CurriculumService.nombreSeguro(".htaccess")).isEqualTo("curriculum.pdf");
	}

	@Test
	void recortaLosNombresDemasiadoLargos() {
		String nombre = CurriculumService.nombreSeguro("a".repeat(400) + ".pdf");

		assertThat(nombre).hasSize(255).endsWith(".pdf");
	}

}

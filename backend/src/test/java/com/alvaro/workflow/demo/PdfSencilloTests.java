package com.alvaro.workflow.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class PdfSencilloTests {

	private static final Charset WIN_ANSI = Charset.forName("windows-1252");

	@Test
	void generaUnPdfConElTextoYLaTablaDeObjetosCorrecta() {
		byte[] pdf = PdfSencillo.conLineas("Ana García", List.of("Diseñadora (UX/UI)", "Inglés C1"));
		String texto = new String(pdf, WIN_ANSI);

		assertThat(texto).startsWith("%PDF-1.4\n").endsWith("%%EOF\n")
				.contains("(Ana García) Tj")
				// Los paréntesis del texto van escapados
				.contains("(Diseñadora \\(UX/UI\\)) Tj");

		// Cada entrada de la tabla apunta al byte donde empieza su objeto
		Matcher entrada = Pattern.compile("(\\d{10}) 00000 n ").matcher(texto);
		int numero = 1;
		while (entrada.find()) {
			int posicion = Integer.parseInt(entrada.group(1));
			assertThat(texto.substring(posicion)).startsWith(numero + " 0 obj\n");
			numero++;
		}
		assertThat(numero - 1).isEqualTo(5);

		Matcher inicioTabla = Pattern.compile("startxref\n(\\d+)\n").matcher(texto);
		assertThat(inicioTabla.find()).isTrue();
		assertThat(texto.substring(Integer.parseInt(inicioTabla.group(1)))).startsWith("xref\n");
	}

}

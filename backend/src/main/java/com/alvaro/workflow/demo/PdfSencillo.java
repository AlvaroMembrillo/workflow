package com.alvaro.workflow.demo;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Escribe a mano un PDF de una página con un título y unas líneas de texto, para los currículos de
 * ejemplo de la demo. Así no hace falta añadir una librería de PDF solo para los datos de muestra.
 */
final class PdfSencillo {

	/** Codificación de la fuente estándar Helvetica con /WinAnsiEncoding: cubre las tildes y la ñ. */
	private static final Charset WIN_ANSI = Charset.forName("windows-1252");

	private PdfSencillo() {
	}

	static byte[] conLineas(String titulo, List<String> lineas) {
		StringBuilder texto = new StringBuilder("BT\n/F1 20 Tf\n72 760 Td\n(").append(escapar(titulo)).append(") Tj\n")
				.append("/F1 11 Tf\n0 -34 Td\n");
		for (String linea : lineas) {
			texto.append('(').append(escapar(linea)).append(") Tj\n0 -17 Td\n");
		}
		texto.append("ET\n");
		byte[] contenido = texto.toString().getBytes(WIN_ANSI);

		List<byte[]> objetos = List.of(
				ascii("<< /Type /Catalog /Pages 2 0 R >>"),
				ascii("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
				ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
						+ "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>"),
				ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"),
				unir(ascii("<< /Length " + contenido.length + " >>\nstream\n"), contenido, ascii("endstream")));

		ByteArrayOutputStream pdf = new ByteArrayOutputStream();
		pdf.writeBytes(ascii("%PDF-1.4\n"));
		// La tabla del final indica en qué byte empieza cada objeto
		List<Integer> posiciones = new ArrayList<>();
		for (int i = 0; i < objetos.size(); i++) {
			posiciones.add(pdf.size());
			pdf.writeBytes(ascii((i + 1) + " 0 obj\n"));
			pdf.writeBytes(objetos.get(i));
			pdf.writeBytes(ascii("\nendobj\n"));
		}
		int inicioTabla = pdf.size();
		StringBuilder tabla = new StringBuilder("xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n");
		for (int posicion : posiciones) {
			tabla.append("%010d 00000 n \n".formatted(posicion));
		}
		tabla.append("trailer\n<< /Size ").append(objetos.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
				.append(inicioTabla).append("\n%%EOF\n");
		pdf.writeBytes(ascii(tabla.toString()));
		return pdf.toByteArray();
	}

	/** En un texto PDF, los paréntesis y la barra invertida van precedidos de una barra invertida. */
	private static String escapar(String texto) {
		return texto.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
	}

	private static byte[] ascii(String texto) {
		return texto.getBytes(StandardCharsets.US_ASCII);
	}

	private static byte[] unir(byte[]... partes) {
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		for (byte[] parte : partes) {
			salida.writeBytes(parte);
		}
		return salida.toByteArray();
	}

}

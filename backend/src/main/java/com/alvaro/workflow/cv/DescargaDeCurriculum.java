package com.alvaro.workflow.cv;

import java.nio.charset.StandardCharsets;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Respuesta HTTP para descargar un currículum. */
public final class DescargaDeCurriculum {

	private DescargaDeCurriculum() {
	}

	/**
	 * El PDF se entrega siempre como descarga, nunca para abrirlo dentro de la web: es un fichero que ha
	 * subido un usuario. Tampoco se guarda en cachés, porque contiene datos personales.
	 */
	public static ResponseEntity<byte[]> de(Curriculum curriculum) {
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.contentLength(curriculum.getTamano())
				.cacheControl(CacheControl.noStore().cachePrivate())
				.header("Content-Disposition", ContentDisposition.attachment()
						.filename(curriculum.getNombreFichero(), StandardCharsets.UTF_8).build().toString())
				.header("Content-Security-Policy", "sandbox")
				.body(curriculum.getContenido());
	}

}

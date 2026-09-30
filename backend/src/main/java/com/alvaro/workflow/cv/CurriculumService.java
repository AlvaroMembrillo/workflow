package com.alvaro.workflow.cv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.alvaro.workflow.common.RecursoNoEncontradoException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurriculumService {

	/** Todos los PDF empiezan por estos bytes. */
	private static final byte[] CABECERA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
	private static final int LONGITUD_MAXIMA_NOMBRE = 255;
	private static final String NOMBRE_POR_DEFECTO = "curriculum.pdf";

	private final CurriculumRepository curriculos;
	private final CvProperties properties;

	/** Guarda el currículum del candidato; si ya tenía uno, lo sustituye. */
	@Transactional
	public CurriculumResponse guardar(UUID usuarioId, MultipartFile fichero) {
		return guardar(usuarioId, fichero.getOriginalFilename(), leer(fichero));
	}

	@Transactional
	public CurriculumResponse guardar(UUID usuarioId, String nombreOriginal, byte[] contenido) {
		if (contenido.length == 0) {
			throw new CurriculumNoValidoException("El fichero está vacío");
		}
		if (contenido.length > properties.tamanoMaximo().toBytes()) {
			throw new CurriculumNoValidoException(
					"El currículum no puede ocupar más de " + properties.tamanoMaximo().toMegabytes() + " MB");
		}
		// Se mira el contenido y no la extensión ni el tipo que declara el navegador, que se pueden falsear
		if (!empiezaPor(contenido, CABECERA_PDF)) {
			throw new CurriculumNoValidoException("El currículum tiene que ser un PDF");
		}

		String nombre = nombreSeguro(nombreOriginal);
		Curriculum curriculum = curriculos.findById(usuarioId)
				.map(existente -> {
					existente.sustituir(nombre, contenido);
					return existente;
				})
				.orElseGet(() -> curriculos.save(new Curriculum(usuarioId, nombre, contenido)));
		return new CurriculumResponse(curriculum.getNombreFichero(), curriculum.getTamano(), curriculum.getFechaSubida());
	}

	@Transactional(readOnly = true)
	public CurriculumResponse resumenDe(UUID usuarioId) {
		return curriculos.resumenDe(usuarioId).map(CurriculumResponse::de).orElseThrow(CurriculumService::sinCurriculum);
	}

	/** El currículum completo, con su contenido, para descargarlo. */
	@Transactional(readOnly = true)
	public Curriculum obtener(UUID usuarioId) {
		return curriculos.findById(usuarioId).orElseThrow(CurriculumService::sinCurriculum);
	}

	@Transactional
	public void borrar(UUID usuarioId) {
		curriculos.deleteById(usuarioId);
	}

	/** El currículum (sin contenido) de cada usuario que tiene uno, con una sola consulta. */
	@Transactional(readOnly = true)
	public Map<UUID, CurriculumResumen> resumenesPorUsuario(Collection<UUID> usuarioIds) {
		if (usuarioIds.isEmpty()) {
			return Map.of();
		}
		return curriculos.resumenesDe(usuarioIds).stream()
				.collect(Collectors.toMap(CurriculumResumen::usuarioId, Function.identity()));
	}

	@Transactional(readOnly = true)
	public Optional<CurriculumResumen> resumenSiExiste(UUID usuarioId) {
		return curriculos.resumenDe(usuarioId);
	}

	private static RecursoNoEncontradoException sinCurriculum() {
		return new RecursoNoEncontradoException("Sin currículum", "No hay ningún currículum subido");
	}

	private static byte[] leer(MultipartFile fichero) {
		try {
			return fichero.getBytes();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static boolean empiezaPor(byte[] contenido, byte[] cabecera) {
		return contenido.length >= cabecera.length
				&& Arrays.equals(contenido, 0, cabecera.length, cabecera, 0, cabecera.length);
	}

	/**
	 * El nombre lo elige quien sube el fichero y después aparece en la cabecera de la descarga: se queda solo
	 * con el nombre (sin carpetas), sin caracteres de control y con extensión .pdf.
	 */
	static String nombreSeguro(String original) {
		String nombre = original == null ? "" : original;
		nombre = nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1);
		nombre = nombre.replaceAll("[\\p{Cntrl}\"<>:|?*]", "").strip();
		if (nombre.isEmpty() || nombre.startsWith(".")) {
			return NOMBRE_POR_DEFECTO;
		}
		if (!nombre.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
			nombre += ".pdf";
		}
		if (nombre.length() > LONGITUD_MAXIMA_NOMBRE) {
			nombre = nombre.substring(0, LONGITUD_MAXIMA_NOMBRE - 4) + ".pdf";
		}
		return nombre;
	}

}

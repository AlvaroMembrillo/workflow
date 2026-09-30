package com.alvaro.workflow.cv;

import java.time.Instant;
import java.util.UUID;

/**
 * Datos de un currículum sin su contenido.
 *
 * @param tamano en bytes
 */
public record CurriculumResumen(UUID usuarioId, String nombreFichero, int tamano, Instant fechaSubida) {
}

package com.alvaro.workflow.moderacion.dto;

import java.time.Instant;
import java.util.UUID;

import com.alvaro.workflow.moderacion.EstadoDenuncia;
import com.alvaro.workflow.moderacion.MotivoDenuncia;
import com.alvaro.workflow.oferta.EstadoOferta;

/**
 * Una denuncia en el panel de moderación, con lo necesario para decidir sin salir de él.
 *
 * @param emailDenunciante null si quien denunció ha borrado su cuenta
 */
public record DenunciaResponse(
		UUID id,
		MotivoDenuncia motivo,
		String detalle,
		EstadoDenuncia estado,
		Instant fechaCreacion,
		Instant fechaResolucion,
		String emailDenunciante,
		OfertaDenunciada oferta) {

	public record OfertaDenunciada(
			UUID id,
			String titulo,
			String descripcion,
			EstadoOferta estado,
			UUID empresaId,
			String empresaNombre,
			String empresaEmail,
			boolean empresaSuspendida) {
	}

}

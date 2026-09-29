package com.alvaro.workflow.oferta.dto;

import java.time.Instant;
import java.util.UUID;

import com.alvaro.workflow.empresa.dto.EmpresaResumen;
import com.alvaro.workflow.oferta.EstadoOferta;
import com.alvaro.workflow.oferta.Modalidad;
import com.alvaro.workflow.oferta.TipoContrato;

public record OfertaResponse(
		UUID id,
		String titulo,
		String descripcion,
		String ubicacion,
		Modalidad modalidad,
		TipoContrato tipoContrato,
		Integer salarioMinimo,
		Integer salarioMaximo,
		EstadoOferta estado,
		EmpresaResumen empresa,
		Instant fechaCreacion,
		Instant fechaActualizacion) {
}

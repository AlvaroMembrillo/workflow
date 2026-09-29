package com.alvaro.workflow.oferta.dto;

import java.util.UUID;

import com.alvaro.workflow.empresa.dto.EmpresaResumen;
import com.alvaro.workflow.oferta.EstadoOferta;

/** Datos mínimos de la oferta para mostrarlos junto a una candidatura. */
public record OfertaResumen(UUID id, String titulo, EstadoOferta estado, EmpresaResumen empresa) {
}

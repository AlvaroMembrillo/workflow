package com.alvaro.workflow.oferta.dto;

import com.alvaro.workflow.oferta.EstadoOferta;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoOfertaRequest(@NotNull(message = "El estado es obligatorio") EstadoOferta estado) {
}

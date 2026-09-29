package com.alvaro.workflow.candidatura.dto;

import java.util.UUID;

/** Datos de contacto del candidato que ve la empresa en las candidaturas recibidas. */
public record CandidatoResumen(UUID id, String nombre, String email) {
}

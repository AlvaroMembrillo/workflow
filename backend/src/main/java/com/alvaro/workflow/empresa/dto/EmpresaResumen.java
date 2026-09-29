package com.alvaro.workflow.empresa.dto;

import java.util.UUID;

/** Datos mínimos de la empresa para mostrarlos junto a sus ofertas. */
public record EmpresaResumen(UUID id, String nombre) {
}

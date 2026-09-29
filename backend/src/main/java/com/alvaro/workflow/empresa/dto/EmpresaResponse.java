package com.alvaro.workflow.empresa.dto;

import java.util.UUID;

public record EmpresaResponse(UUID id, String nombre, String descripcion, String sitioWeb, String ubicacion) {
}

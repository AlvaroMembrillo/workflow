package com.alvaro.workflow.usuario;

import java.time.Instant;
import java.util.UUID;

public record UsuarioResponse(UUID id, String email, String nombre, Rol rol, Instant fechaCreacion) {
}

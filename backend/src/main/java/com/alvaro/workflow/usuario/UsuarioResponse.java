package com.alvaro.workflow.usuario;

import java.time.Instant;
import java.util.UUID;

/**
 * @param emailVerificado si ha confirmado que el email es suyo con el enlace enviado por correo
 * @param avisosPorCorreo si quiere recibir avisos por correo (nueva candidatura, cambio de estado)
 */
public record UsuarioResponse(UUID id, String email, String nombre, Rol rol, Instant fechaCreacion,
		boolean emailVerificado, boolean avisosPorCorreo) {
}

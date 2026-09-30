package com.alvaro.workflow.usuario;

import jakarta.validation.constraints.NotNull;

public record PreferenciasRequest(@NotNull(message = "Indica si quieres recibir avisos por correo") Boolean avisosPorCorreo) {
}

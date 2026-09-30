package com.alvaro.workflow.cuenta;

import jakarta.validation.constraints.NotBlank;

public record BajaRequest(@NotBlank(message = "Escribe tu contraseña para confirmar") String password) {
}

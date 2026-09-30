package com.alvaro.workflow.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** @param token el que viaja en el enlace enviado por correo */
public record EnlaceRequest(@NotBlank(message = "Falta el token del enlace") String token) {
}

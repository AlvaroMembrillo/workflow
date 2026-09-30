package com.alvaro.workflow.cuenta;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/** @param borrarSinVerificarTras tiempo tras el que se borra una cuenta que no ha confirmado su email */
@Validated
@ConfigurationProperties(prefix = "app.cuentas")
public record CuentasProperties(@NotNull Duration borrarSinVerificarTras) {
}

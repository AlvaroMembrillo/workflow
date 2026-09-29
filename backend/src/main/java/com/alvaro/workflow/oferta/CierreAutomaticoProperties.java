package com.alvaro.workflow.oferta;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @param inactividad tiempo sin cambios tras el que una oferta abierta se cierra sola
 * @param cron cuándo se ejecuta el cierre (hora de Madrid)
 */
@Validated
@ConfigurationProperties(prefix = "app.ofertas.cierre-automatico")
public record CierreAutomaticoProperties(@NotNull Duration inactividad, @NotBlank String cron) {
}

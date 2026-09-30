package com.alvaro.workflow.legal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Identidad de quien responde del portal (persona o empresa), que la ley obliga a mostrar en el aviso
 * legal y en la política de privacidad.
 */
@ConfigurationProperties(prefix = "app.legal")
public record LegalProperties(String titular, String nif, String domicilio) {
}

package com.alvaro.workflow.correo;

/**
 * Un correo de texto plano para un único destinatario.
 *
 * @param para dirección del destinatario
 */
public record Mensaje(String para, String asunto, String texto) {
}

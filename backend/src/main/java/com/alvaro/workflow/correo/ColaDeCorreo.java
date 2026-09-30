package com.alvaro.workflow.correo;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Envía los correos fuera de la petición que los origina: después de confirmar la transacción (si se
 * deshace, no sale ningún correo) y en otro hilo, para que un servidor de correo lento o caído no
 * retrase ni haga fallar el registro o el cambio de estado de una candidatura.
 * <p>
 * Si el envío falla, el correo se pierde y queda anotado en el registro. El usuario puede pedir otro
 * enlace de verificación o de recuperación; para no perder avisos haría falta una cola persistente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ColaDeCorreo {

	private final ApplicationEventPublisher eventos;
	private final EnvioDeCorreo envio;

	public void encolar(Mensaje mensaje) {
		eventos.publishEvent(mensaje);
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void enviar(Mensaje mensaje) {
		try {
			envio.enviar(mensaje);
		}
		catch (MailException ex) {
			// No se anota el destinatario: es un dato personal
			log.error("No se ha podido enviar el correo \"{}\"", mensaje.asunto(), ex);
		}
	}

}

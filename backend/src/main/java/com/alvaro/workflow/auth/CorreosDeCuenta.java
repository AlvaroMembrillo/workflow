package com.alvaro.workflow.auth;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.alvaro.workflow.correo.CorreoProperties;
import com.alvaro.workflow.correo.Mensaje;
import com.alvaro.workflow.usuario.Usuario;

import lombok.RequiredArgsConstructor;

/**
 * Textos de los correos sobre la cuenta. El token va en el fragmento del enlace (detrás de #): el
 * navegador no lo envía al servidor, así que no queda en los registros de acceso de nginx.
 */
@Component
@RequiredArgsConstructor
class CorreosDeCuenta {

	private final CorreoProperties correo;
	private final EnlacesProperties enlaces;

	Mensaje verificarEmail(Usuario usuario, String token) {
		return new Mensaje(usuario.getEmail(), "Confirma tu email en Workflow", """
				Hola, %s:

				Para terminar de crear tu cuenta en Workflow, confirma que este email es tuyo abriendo este enlace:

				%s

				El enlace caduca en %s. Si no has creado una cuenta en Workflow, ignora este mensaje.
				""".formatted(usuario.getNombre(), correo.enlace("/verificar-email#" + token),
				enTexto(enlaces.validezVerificacion())));
	}

	Mensaje restablecerPassword(Usuario usuario, String token) {
		return new Mensaje(usuario.getEmail(), "Cambia tu contraseña de Workflow", """
				Hola, %s:

				Hemos recibido una petición para cambiar la contraseña de tu cuenta. Elige una nueva en este enlace:

				%s

				El enlace caduca en %s y solo se puede usar una vez. Si no lo has pedido tú, ignora este \
				mensaje: tu contraseña sigue siendo la misma.
				""".formatted(usuario.getNombre(), correo.enlace("/restablecer#" + token),
				enTexto(enlaces.validezRestablecimiento())));
	}

	Mensaje passwordCambiada(Usuario usuario) {
		return new Mensaje(usuario.getEmail(), "Has cambiado tu contraseña de Workflow", """
				Hola, %s:

				La contraseña de tu cuenta se acaba de cambiar.

				Si no has sido tú, cámbiala cuanto antes desde este enlace:

				%s
				""".formatted(usuario.getNombre(), correo.enlace("/recuperar")));
	}

	/** "48 horas", "1 hora", "30 minutos": como lo diría una persona. */
	static String enTexto(Duration duracion) {
		if (duracion.toHours() >= 1) {
			long horas = duracion.toHours();
			return horas == 1 ? "1 hora" : horas + " horas";
		}
		long minutos = Math.max(1, duracion.toMinutes());
		return minutos == 1 ? "1 minuto" : minutos + " minutos";
	}

}

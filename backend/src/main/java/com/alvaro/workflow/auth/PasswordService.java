package com.alvaro.workflow.auth;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.common.CampoNoValidoException;
import com.alvaro.workflow.correo.ColaDeCorreo;
import com.alvaro.workflow.usuario.Usuario;
import com.alvaro.workflow.usuario.UsuarioRepository;
import com.alvaro.workflow.usuario.UsuarioService;

import lombok.RequiredArgsConstructor;

/** Cambio de contraseña: con un enlace enviado por correo si se ha olvidado, o con la actual. */
@Service
@RequiredArgsConstructor
public class PasswordService {

	private final UsuarioRepository usuarios;
	private final UsuarioService usuarioService;
	private final PasswordEncoder passwordEncoder;
	private final TokensDeUnUso tokens;
	private final CorreosDeCuenta correos;
	private final ColaDeCorreo colaDeCorreo;
	private final SesionService sesiones;

	/**
	 * Envía un enlace para cambiar la contraseña si existe una cuenta con ese email. No indica si existe:
	 * quien llama recibe siempre la misma respuesta, para que no se pueda averiguar qué emails están registrados.
	 */
	@Transactional
	public void solicitarCambio(String email) {
		usuarios.findByEmail(Usuario.normalizarEmail(email)).ifPresent(usuario -> {
			String token = tokens.crear(usuario, TipoDeToken.RESTABLECER_PASSWORD);
			colaDeCorreo.encolar(correos.restablecerPassword(usuario, token));
		});
	}

	@Transactional
	public void restablecer(String token, String passwordNueva) {
		Usuario usuario = tokens.usar(token, TipoDeToken.RESTABLECER_PASSWORD);
		usuario.cambiarPassword(passwordEncoder.encode(passwordNueva));
		// Ha abierto un enlace enviado a su email: queda demostrado que es suyo
		usuario.verificarEmail();
		// Quien tuviera la sesión abierta con la contraseña anterior se queda fuera
		sesiones.cerrarTodas(usuario.getId());
		colaDeCorreo.encolar(correos.passwordCambiada(usuario));
	}

	/**
	 * Cambia la contraseña y cierra las sesiones de los demás dispositivos.
	 *
	 * @return el token de refresco de la sesión nueva para este dispositivo
	 */
	@Transactional
	public String cambiar(UUID usuarioId, String passwordActual, String passwordNueva) {
		Usuario usuario = usuarioService.buscarPorId(usuarioId);
		if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
			throw new CampoNoValidoException("passwordActual", "La contraseña actual no es correcta");
		}
		usuario.cambiarPassword(passwordEncoder.encode(passwordNueva));
		colaDeCorreo.encolar(correos.passwordCambiada(usuario));
		sesiones.cerrarTodas(usuarioId);
		return sesiones.abrir(usuario);
	}

}

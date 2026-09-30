package com.alvaro.workflow.auth;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.correo.ColaDeCorreo;
import com.alvaro.workflow.limites.LimitadorDeIntentos;
import com.alvaro.workflow.limites.LimitesProperties;
import com.alvaro.workflow.usuario.Usuario;
import com.alvaro.workflow.usuario.UsuarioService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificacionEmailService {

	/** Los correos de verificación y de recuperación que se piden para una misma dirección comparten límite. */
	static final String CORREOS_POR_DESTINATARIO = "correos-destinatario";

	private final TokensDeUnUso tokens;
	private final CorreosDeCuenta correos;
	private final ColaDeCorreo colaDeCorreo;
	private final UsuarioService usuarioService;
	private final LimitadorDeIntentos limitador;
	private final LimitesProperties limites;

	/** Envía al usuario un enlace para confirmar que el email es suyo. */
	@Transactional
	public void enviarEnlace(Usuario usuario) {
		String token = tokens.crear(usuario, TipoDeToken.VERIFICAR_EMAIL);
		colaDeCorreo.encolar(correos.verificarEmail(usuario, token));
	}

	/** El usuario pide otro enlace porque el anterior ha caducado o no le ha llegado. */
	@Transactional
	public void reenviar(UUID usuarioId) {
		Usuario usuario = usuarioService.buscarPorId(usuarioId);
		if (!usuario.isEmailVerificado()) {
			limitador.contar(CORREOS_POR_DESTINATARIO, usuario.getEmail(), limites.correosPorDestinatario());
			enviarEnlace(usuario);
		}
	}

	@Transactional
	public void verificar(String token) {
		// Abrir el enlace por segunda vez no es un error: el email ya está verificado
		boolean yaVerificado = tokens.usuarioDe(token, TipoDeToken.VERIFICAR_EMAIL)
				.filter(Usuario::isEmailVerificado)
				.isPresent();
		if (!yaVerificado) {
			tokens.usar(token, TipoDeToken.VERIFICAR_EMAIL).verificarEmail();
		}
	}

}

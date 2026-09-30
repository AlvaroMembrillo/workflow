package com.alvaro.workflow.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.usuario.Usuario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Crea y comprueba los enlaces de un solo uso (verificar el email, cambiar la contraseña).
 * En la base de datos solo se guarda el hash de cada token ({@link TokensAleatorios}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
class TokensDeUnUso {

	/** Los tokens caducados se conservan unos días por si hay que investigar un problema. */
	private static final Duration CONSERVACION = Duration.ofDays(7);

	private final TokenDeUnUsoRepository tokens;
	private final EnlacesProperties properties;

	/** Crea un token nuevo e invalida los anteriores del mismo tipo. Devuelve el token que va en el enlace. */
	@Transactional
	public String crear(Usuario usuario, TipoDeToken tipo) {
		tokens.borrarDelUsuario(usuario.getId(), tipo);

		String token = TokensAleatorios.nuevo();
		tokens.save(new TokenDeUnUso(usuario, tipo, TokensAleatorios.hash(token), properties.validez(tipo)));
		return token;
	}

	/**
	 * Gasta el token y devuelve el usuario al que pertenece.
	 *
	 * @throws EnlaceNoValidoException si no existe, ha caducado o ya se ha usado
	 */
	@Transactional
	public Usuario usar(String token, TipoDeToken tipo) {
		String hash = TokensAleatorios.hash(token);
		if (tokens.usar(hash, tipo, Instant.now()) == 0) {
			throw new EnlaceNoValidoException();
		}
		return tokens.findByHashAndTipo(hash, tipo).orElseThrow(EnlaceNoValidoException::new).getUsuario();
	}

	/** El usuario de un token, aunque esté usado o caducado. */
	@Transactional(readOnly = true)
	public Optional<Usuario> usuarioDe(String token, TipoDeToken tipo) {
		return tokens.findByHashAndTipo(TokensAleatorios.hash(token), tipo).map(TokenDeUnUso::getUsuario);
	}

	@Scheduled(cron = "0 30 3 * * *", zone = "Europe/Madrid")
	@Transactional
	public void borrarCaducados() {
		int borrados = tokens.borrarCaducadosAntesDe(Instant.now().minus(CONSERVACION));
		if (borrados > 0) {
			log.info("Borrados {} enlaces de un solo uso caducados", borrados);
		}
	}

}

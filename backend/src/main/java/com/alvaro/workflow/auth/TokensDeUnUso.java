package com.alvaro.workflow.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.usuario.Usuario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Crea y comprueba los enlaces de un solo uso (verificar el email, cambiar la contraseña).
 * El token es aleatorio (256 bits) y en la base de datos solo se guarda su SHA-256.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class TokensDeUnUso {

	private static final int BYTES_DEL_TOKEN = 32;
	/** Los tokens caducados se conservan unos días por si hay que investigar un problema. */
	private static final Duration CONSERVACION = Duration.ofDays(7);

	private final TokenDeUnUsoRepository tokens;
	private final EnlacesProperties properties;
	private final SecureRandom aleatorio = new SecureRandom();

	/** Crea un token nuevo e invalida los anteriores del mismo tipo. Devuelve el token que va en el enlace. */
	@Transactional
	public String crear(Usuario usuario, TipoDeToken tipo) {
		tokens.borrarDelUsuario(usuario.getId(), tipo);

		byte[] bytes = new byte[BYTES_DEL_TOKEN];
		aleatorio.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		tokens.save(new TokenDeUnUso(usuario, tipo, hash(token), properties.validez(tipo)));
		return token;
	}

	/**
	 * Gasta el token y devuelve el usuario al que pertenece.
	 *
	 * @throws EnlaceNoValidoException si no existe, ha caducado o ya se ha usado
	 */
	@Transactional
	public Usuario usar(String token, TipoDeToken tipo) {
		String hash = hash(token);
		if (tokens.usar(hash, tipo, Instant.now()) == 0) {
			throw new EnlaceNoValidoException();
		}
		return tokens.findByHashAndTipo(hash, tipo).orElseThrow(EnlaceNoValidoException::new).getUsuario();
	}

	/** El usuario de un token, aunque esté usado o caducado. */
	@Transactional(readOnly = true)
	public Optional<Usuario> usuarioDe(String token, TipoDeToken tipo) {
		return tokens.findByHashAndTipo(hash(token), tipo).map(TokenDeUnUso::getUsuario);
	}

	@Scheduled(cron = "0 30 3 * * *", zone = "Europe/Madrid")
	@Transactional
	public void borrarCaducados() {
		int borrados = tokens.borrarCaducadosAntesDe(Instant.now().minus(CONSERVACION));
		if (borrados > 0) {
			log.info("Borrados {} enlaces de un solo uso caducados", borrados);
		}
	}

	private static String hash(String token) {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}

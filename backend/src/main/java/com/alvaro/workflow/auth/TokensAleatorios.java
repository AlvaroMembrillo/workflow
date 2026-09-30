package com.alvaro.workflow.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens opacos para enlaces de un solo uso y sesiones: 256 bits aleatorios que se entregan al usuario,
 * mientras que en la base de datos solo se guarda su SHA-256. Como el token ya es aleatorio e
 * imposible de adivinar, no hace falta un hash lento como el de las contraseñas.
 */
final class TokensAleatorios {

	private static final int BYTES = 32;
	private static final SecureRandom ALEATORIO = new SecureRandom();

	private TokensAleatorios() {
	}

	static String nuevo() {
		byte[] bytes = new byte[BYTES];
		ALEATORIO.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String hash(String token) {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}

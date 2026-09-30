package com.alvaro.workflow.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.usuario.Usuario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sesiones largas con tokens de refresco rotatorios: cada token solo se puede canjear una vez por un token
 * de acceso nuevo y el siguiente token de refresco. Si alguien presenta un token ya canjeado, puede que lo
 * hayan robado: se cierra toda la sesión, la del ladrón y la del usuario, que tendrá que volver a entrar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SesionService {

	/** Los tokens caducados se conservan unos días para poder detectar su reutilización. */
	private static final Duration CONSERVACION = Duration.ofDays(7);

	private final TokenDeRefrescoRepository tokens;
	private final SesionProperties properties;

	/**
	 * @param tokenDeRefresco el token que sustituye al canjeado, o null si la cookie actual sigue valiendo
	 */
	public record Renovacion(Usuario usuario, String tokenDeRefresco) {
	}

	/** Abre una sesión nueva tras comprobar las credenciales. Devuelve el token de refresco. */
	@Transactional
	public String abrir(Usuario usuario) {
		return crear(usuario, UUID.randomUUID());
	}

	/**
	 * Canjea un token de refresco.
	 *
	 * @throws SesionNoValidaException si el token no existe, ha caducado, está revocado o ya se canjeó
	 */
	// Si se detecta una reutilización, la revocación de la sesión se tiene que guardar aunque se lance la excepción
	@Transactional(noRollbackFor = SesionNoValidaException.class)
	public Renovacion renovar(String token) {
		String hash = TokensAleatorios.hash(token);
		Instant ahora = Instant.now();
		if (tokens.canjear(hash, ahora) == 1) {
			TokenDeRefresco canjeado = tokens.findByHash(hash).orElseThrow(SesionNoValidaException::new);
			return new Renovacion(canjeado.getUsuario(), crear(canjeado.getUsuario(), canjeado.getFamilia()));
		}

		TokenDeRefresco presentado = tokens.findByHash(hash).orElseThrow(SesionNoValidaException::new);
		if (presentado.sigueEnPie(ahora) && presentado.getFechaUso() != null) {
			if (presentado.getFechaUso().plus(properties.margenDeReutilizacion()).isAfter(ahora)) {
				// Otra pestaña acaba de renovar con esta misma cookie: el navegador ya tiene la nueva
				return new Renovacion(presentado.getUsuario(), null);
			}
			log.warn("Reutilización de un token de refresco ya canjeado: se cierra la sesión {}", presentado.getFamilia());
			tokens.revocarFamilia(presentado.getFamilia(), ahora);
		}
		throw new SesionNoValidaException();
	}

	/** Cierra la sesión a la que pertenece el token. Si el token no existe, no hace nada. */
	@Transactional
	public void cerrar(String token) {
		tokens.findByHash(TokensAleatorios.hash(token))
				.ifPresent(encontrado -> tokens.revocarFamilia(encontrado.getFamilia(), Instant.now()));
	}

	/** Cierra todas las sesiones del usuario, en todos sus dispositivos. */
	@Transactional
	public void cerrarTodas(UUID usuarioId) {
		tokens.revocarDelUsuario(usuarioId, Instant.now());
	}

	@Scheduled(cron = "0 45 3 * * *", zone = "Europe/Madrid")
	@Transactional
	public void borrarCaducados() {
		int borrados = tokens.borrarCaducadosAntesDe(Instant.now().minus(CONSERVACION));
		if (borrados > 0) {
			log.info("Borrados {} tokens de refresco caducados", borrados);
		}
	}

	private String crear(Usuario usuario, UUID familia) {
		String token = TokensAleatorios.nuevo();
		tokens.save(new TokenDeRefresco(usuario, familia, TokensAleatorios.hash(token), properties.duracion()));
		return token;
	}

}

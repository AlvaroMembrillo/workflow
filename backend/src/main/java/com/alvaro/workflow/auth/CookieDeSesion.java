package com.alvaro.workflow.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * La cookie que lleva el token de refresco.
 * <ul>
 * <li>{@code HttpOnly}: el JavaScript de la página no puede leerla, así que un XSS no puede llevársela.</li>
 * <li>{@code SameSite=Strict}: el navegador no la envía en peticiones que vienen de otra web (CSRF).</li>
 * <li>{@code Path=/api/auth}: solo viaja a los endpoints de sesión, no con cada petición a la API.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CookieDeSesion {

	public static final String NOMBRE = "workflow_refresco";
	private static final String RUTA = "/api/auth";

	private final SesionProperties properties;

	public ResponseCookie con(String tokenDeRefresco) {
		return cookie(tokenDeRefresco, properties.duracion());
	}

	/** Cookie vacía y ya caducada: el navegador borra la que tenía. */
	public ResponseCookie borrada() {
		return cookie("", Duration.ZERO);
	}

	private ResponseCookie cookie(String valor, Duration duracion) {
		return ResponseCookie.from(NOMBRE, valor)
				.httpOnly(true)
				.secure(properties.cookieSegura())
				.sameSite("Strict")
				.path(RUTA)
				.maxAge(duracion)
				.build();
	}

}

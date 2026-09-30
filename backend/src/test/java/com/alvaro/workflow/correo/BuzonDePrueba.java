package com.alvaro.workflow.correo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sustituye al servidor de correo en los tests: guarda los mensajes en memoria para poder comprobarlos
 * y sacar de ellos los enlaces, como haría el usuario al abrir el correo.
 */
public class BuzonDePrueba implements EnvioDeCorreo {

	private static final Pattern TOKEN_DEL_ENLACE = Pattern.compile("#([A-Za-z0-9_-]+)");

	private final List<Mensaje> mensajes = new CopyOnWriteArrayList<>();

	@Override
	public void enviar(Mensaje mensaje) {
		mensajes.add(mensaje);
	}

	/** Los mensajes enviados a una dirección, del más antiguo al más reciente. */
	public List<Mensaje> para(String email) {
		return mensajes.stream().filter(mensaje -> mensaje.para().equals(email)).toList();
	}

	public Mensaje ultimoPara(String email) {
		List<Mensaje> recibidos = para(email);
		if (recibidos.isEmpty()) {
			throw new AssertionError("No se ha enviado ningún correo a " + email);
		}
		return recibidos.getLast();
	}

	/** El token del enlace del último mensaje enviado a una dirección. */
	public String tokenDelUltimoEnlacePara(String email) {
		Matcher enlace = TOKEN_DEL_ENLACE.matcher(ultimoPara(email).texto());
		if (!enlace.find()) {
			throw new AssertionError("El último correo a " + email + " no tiene un enlace con token");
		}
		return enlace.group(1);
	}

}

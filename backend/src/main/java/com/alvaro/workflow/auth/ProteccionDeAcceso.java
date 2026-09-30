package com.alvaro.workflow.auth;

import java.util.function.Supplier;

import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.alvaro.workflow.limites.LimitadorDeIntentos;
import com.alvaro.workflow.limites.LimitesProperties;
import com.alvaro.workflow.usuario.Usuario;

import lombok.RequiredArgsConstructor;

/** Límites de intentos de los endpoints públicos de acceso, que son los que un atacante puede repetir sin cuenta. */
@Component
@RequiredArgsConstructor
class ProteccionDeAcceso {

	private static final String REGISTRO_POR_IP = "registro-ip";
	private static final String LOGIN_POR_IP = "login-ip";
	private static final String FALLOS_DE_LOGIN = "login-fallos";
	private static final String CORREOS_POR_IP = "correos-ip";

	private final LimitadorDeIntentos limitador;
	private final LimitesProperties limites;

	void alRegistrarse(String ip) {
		limitador.contar(REGISTRO_POR_IP, ip, limites.registroPorIp());
	}

	/**
	 * Ejecuta el inicio de sesión contando los fallos. La cuenta de fallos es por email e IP: así quien
	 * prueba contraseñas desde una IP no deja sin poder entrar al dueño de la cuenta, que llega desde otra.
	 */
	<T> T alEntrar(String email, String ip, Supplier<T> login) {
		limitador.contar(LOGIN_POR_IP, ip, limites.loginPorIp());
		String quien = Usuario.normalizarEmail(email) + "|" + ip;
		limitador.comprobar(FALLOS_DE_LOGIN, quien, limites.fallosDeLogin());
		try {
			T resultado = login.get();
			limitador.olvidar(FALLOS_DE_LOGIN, quien);
			return resultado;
		}
		catch (AuthenticationException ex) {
			limitador.permite(FALLOS_DE_LOGIN, quien, limites.fallosDeLogin());
			throw ex;
		}
	}

	void alPedirRecuperacion(String ip) {
		limitador.contar(CORREOS_POR_IP, ip, limites.correosPorIp());
	}

}

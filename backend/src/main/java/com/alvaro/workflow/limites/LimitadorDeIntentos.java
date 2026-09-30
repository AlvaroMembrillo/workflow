package com.alvaro.workflow.limites;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alvaro.workflow.limites.LimitesProperties.Limite;

/**
 * Cuenta cuántas veces se repite una acción (por IP, por email...) para frenar ataques de fuerza bruta y
 * el envío masivo de correos. Usa ventanas fijas: el primer intento abre una ventana y, cuando pasa su
 * duración, la cuenta vuelve a empezar.
 * <p>
 * Las cuentas se guardan en memoria, así que se pierden al reiniciar y no se comparten entre instancias.
 * Con una sola instancia es suficiente; con varias habría que llevarlas a Redis.
 */
@Component
public class LimitadorDeIntentos {

	private record Ventana(Instant fin, int intentos) {
	}

	private final ConcurrentMap<String, Ventana> ventanas = new ConcurrentHashMap<>();
	private final InstantSource reloj;

	public LimitadorDeIntentos() {
		this(InstantSource.system());
	}

	LimitadorDeIntentos(InstantSource reloj) {
		this.reloj = reloj;
	}

	/**
	 * Anota un intento.
	 *
	 * @param accion nombre de lo que se limita, por ejemplo "registro-ip"
	 * @param quien a quién se le cuenta: una IP, un email...
	 * @throws DemasiadosIntentosException si con este intento se supera el límite
	 */
	public void contar(String accion, String quien, Limite limite) {
		Instant ahora = reloj.instant();
		Ventana ventana = ventanas.compute(clave(accion, quien), (clave, actual) -> actual == null || !actual.fin().isAfter(ahora)
				? new Ventana(ahora.plus(limite.ventana()), 1)
				: new Ventana(actual.fin(), actual.intentos() + 1));
		if (ventana.intentos() > limite.maximo()) {
			throw new DemasiadosIntentosException(Duration.between(ahora, ventana.fin()));
		}
	}

	/** Como {@link #contar}, pero en lugar de lanzar una excepción devuelve si el intento cabe en el límite. */
	public boolean permite(String accion, String quien, Limite limite) {
		try {
			contar(accion, quien, limite);
			return true;
		}
		catch (DemasiadosIntentosException ex) {
			return false;
		}
	}

	/**
	 * Comprueba el límite sin anotar ningún intento.
	 *
	 * @throws DemasiadosIntentosException si ya se ha alcanzado el límite
	 */
	public void comprobar(String accion, String quien, Limite limite) {
		Instant ahora = reloj.instant();
		Ventana ventana = ventanas.get(clave(accion, quien));
		if (ventana != null && ventana.fin().isAfter(ahora) && ventana.intentos() >= limite.maximo()) {
			throw new DemasiadosIntentosException(Duration.between(ahora, ventana.fin()));
		}
	}

	/** Pone la cuenta a cero, por ejemplo al entrar con la contraseña correcta. */
	public void olvidar(String accion, String quien) {
		ventanas.remove(clave(accion, quien));
	}

	/** Borra las ventanas que ya han terminado, para que el mapa no crezca sin límite. */
	@Scheduled(fixedDelay = 10, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
	void limpiar() {
		Instant ahora = reloj.instant();
		ventanas.values().removeIf(ventana -> !ventana.fin().isAfter(ahora));
	}

	int ventanasAbiertas() {
		return ventanas.size();
	}

	private static String clave(String accion, String quien) {
		return accion + ":" + quien;
	}

}

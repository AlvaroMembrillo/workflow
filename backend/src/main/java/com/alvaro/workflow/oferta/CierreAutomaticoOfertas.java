package com.alvaro.workflow.oferta;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cierra cada noche las ofertas que llevan mucho tiempo sin cambios, para que el listado no se llene
 * de vacantes que ya no están activas (las "ofertas fantasma" de otros portales).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CierreAutomaticoOfertas {

	private final OfertaRepository ofertas;
	private final CierreAutomaticoProperties properties;

	/** @return número de ofertas cerradas */
	@Scheduled(cron = "${app.ofertas.cierre-automatico.cron}", zone = "Europe/Madrid")
	@Transactional
	public int cerrarOfertasSinActividad() {
		Instant ahora = Instant.now();
		int cerradas = ofertas.cerrarSinActividadDesde(ahora.minus(properties.inactividad()), ahora);
		if (cerradas > 0) {
			log.info("Cerradas {} ofertas sin actividad desde hace {} días", cerradas, properties.inactividad().toDays());
		}
		return cerradas;
	}

}

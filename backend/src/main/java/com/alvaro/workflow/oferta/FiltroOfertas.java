package com.alvaro.workflow.oferta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Filtros opcionales del listado público de ofertas. Solo se devuelven ofertas abiertas.
 */
public record FiltroOfertas(
		@Parameter(description = "Texto a buscar en el título o la descripción") String texto,
		@Parameter(description = "Parte del nombre de la ubicación, por ejemplo \"Madrid\"") String ubicacion,
		Modalidad modalidad,
		TipoContrato tipoContrato) {

	private static final char ESCAPE = '\\';

	Specification<Oferta> comoEspecificacion() {
		List<Specification<Oferta>> condiciones = new ArrayList<>();
		condiciones.add((oferta, consulta, cb) -> cb.equal(oferta.get("estado"), EstadoOferta.ABIERTA));

		if (tieneTexto(texto)) {
			String patron = patron(texto);
			condiciones.add((oferta, consulta, cb) -> cb.or(
					cb.like(cb.lower(oferta.get("titulo")), patron, ESCAPE),
					cb.like(cb.lower(oferta.get("descripcion")), patron, ESCAPE)));
		}
		if (tieneTexto(ubicacion)) {
			String patron = patron(ubicacion);
			condiciones.add((oferta, consulta, cb) -> cb.like(cb.lower(oferta.get("ubicacion")), patron, ESCAPE));
		}
		if (modalidad != null) {
			condiciones.add((oferta, consulta, cb) -> cb.equal(oferta.get("modalidad"), modalidad));
		}
		if (tipoContrato != null) {
			condiciones.add((oferta, consulta, cb) -> cb.equal(oferta.get("tipoContrato"), tipoContrato));
		}
		return Specification.allOf(condiciones);
	}

	private static boolean tieneTexto(String valor) {
		return valor != null && !valor.isBlank();
	}

	/** Busca sin distinguir mayúsculas y trata % y _ como texto normal, no como comodines. */
	private static String patron(String valor) {
		String escapado = valor.strip().toLowerCase(Locale.ROOT)
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
		return "%" + escapado + "%";
	}

}

package com.alvaro.workflow.cuenta;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.alvaro.workflow.candidatura.EstadoCandidatura;
import com.alvaro.workflow.moderacion.DenunciaService.DenunciaHecha;
import com.alvaro.workflow.oferta.EstadoOferta;
import com.alvaro.workflow.oferta.Modalidad;
import com.alvaro.workflow.oferta.TipoContrato;
import com.alvaro.workflow.usuario.Rol;

/**
 * Copia de todos los datos personales de un usuario, para sus derechos de acceso y de portabilidad (RGPD,
 * artículos 15 y 20). El fichero del currículum se descarga aparte; aquí solo van sus datos.
 *
 * @param empresa el perfil de empresa; null si la cuenta es de candidato
 * @param curriculum null si no ha subido ninguno
 */
public record MisDatos(
		Instant generado,
		Cuenta cuenta,
		PerfilDeEmpresa empresa,
		List<OfertaPublicada> ofertas,
		Curriculum curriculum,
		List<CandidaturaEnviada> candidaturas,
		List<DenunciaHecha> denuncias) {

	public record Cuenta(UUID id, String email, String nombre, Rol rol, Instant fechaCreacion,
			Instant emailVerificadoEn, Instant condicionesAceptadasEn, boolean avisosPorCorreo) {
	}

	public record PerfilDeEmpresa(String nombre, String descripcion, String sitioWeb, String ubicacion,
			Instant fechaCreacion) {
	}

	public record OfertaPublicada(String titulo, String descripcion, String ubicacion, Modalidad modalidad,
			TipoContrato tipoContrato, Integer salarioMinimo, Integer salarioMaximo, EstadoOferta estado,
			Instant fechaCreacion) {
	}

	/** @param tamano en bytes */
	public record Curriculum(String nombreFichero, int tamano, Instant fechaSubida) {
	}

	public record CandidaturaEnviada(String oferta, String empresa, EstadoCandidatura estado, String cartaPresentacion,
			Instant fechaCreacion, Instant fechaRevision, Instant fechaResolucion) {
	}

}

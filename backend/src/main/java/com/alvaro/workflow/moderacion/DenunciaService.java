package com.alvaro.workflow.moderacion;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.auth.SesionService;
import com.alvaro.workflow.common.ConflictoException;
import com.alvaro.workflow.common.PeticionNoValidaException;
import com.alvaro.workflow.common.RecursoNoEncontradoException;
import com.alvaro.workflow.empresa.Empresa;
import com.alvaro.workflow.empresa.EmpresaService;
import com.alvaro.workflow.moderacion.dto.DenunciaRequest;
import com.alvaro.workflow.moderacion.dto.DenunciaResponse;
import com.alvaro.workflow.moderacion.dto.DenunciaResponse.OfertaDenunciada;
import com.alvaro.workflow.moderacion.dto.ResolucionRequest.Accion;
import com.alvaro.workflow.oferta.Oferta;
import com.alvaro.workflow.oferta.OfertaNoEncontradaException;
import com.alvaro.workflow.oferta.OfertaService;
import com.alvaro.workflow.usuario.Usuario;
import com.alvaro.workflow.usuario.UsuarioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DenunciaService {

	private final DenunciaRepository denuncias;
	private final OfertaService ofertaService;
	private final UsuarioService usuarioService;
	private final EmpresaService empresaService;
	private final SesionService sesiones;
	private final AvisosDeModeracion avisos;

	@Transactional
	public void denunciar(UUID usuarioId, UUID ofertaId, DenunciaRequest request) {
		Oferta oferta = ofertaService.oferta(ofertaId);
		if (oferta.estaRetirada()) {
			throw new OfertaNoEncontradaException(ofertaId);
		}
		if (oferta.esDeLaEmpresaDe(usuarioId)) {
			throw new PeticionNoValidaException("Denuncia no válida", "No puedes denunciar tu propia oferta");
		}
		// Si llegan dos a la vez, la restricción única de la tabla impide el duplicado (409)
		if (denuncias.existsByOfertaIdAndDenuncianteId(ofertaId, usuarioId)) {
			throw new ConflictoException("Denuncia repetida", "Ya has denunciado esta oferta. La estamos revisando.");
		}

		String detalle = request.detalle() == null || request.detalle().isBlank() ? null : request.detalle().strip();
		Denuncia denuncia = denuncias.save(
				new Denuncia(oferta, usuarioService.buscarPorId(usuarioId), request.motivo(), detalle));
		avisos.denunciaRecibida(denuncia);
	}

	@Transactional(readOnly = true)
	public Page<DenunciaResponse> buscar(EstadoDenuncia estado, Pageable pageable) {
		return denuncias.findByEstado(estado, pageable).map(DenunciaService::respuesta);
	}

	@Transactional
	public DenunciaResponse resolver(UUID administradorId, UUID denunciaId, Accion accion) {
		Denuncia denuncia = denuncias.findById(denunciaId).orElseThrow(
				() -> new RecursoNoEncontradoException("Denuncia no encontrada", "No existe ninguna denuncia con id " + denunciaId));
		if (!denuncia.estaPendiente()) {
			throw new ConflictoException("Denuncia ya resuelta", "Otra persona ya ha resuelto esta denuncia");
		}

		Oferta oferta = denuncia.getOferta();
		switch (accion) {
			case DESESTIMAR -> denuncia.resolver(EstadoDenuncia.DESESTIMADA);
			case RETIRAR_OFERTA -> {
				oferta.retirar();
				// Las demás denuncias de la misma oferta quedan resueltas con ella
				denuncias.findByOfertaIdAndEstado(oferta.getId(), EstadoDenuncia.PENDIENTE)
						.forEach(pendiente -> pendiente.resolver(EstadoDenuncia.ACEPTADA));
				denuncia.resolver(EstadoDenuncia.ACEPTADA);
				avisos.ofertaRetirada(oferta);
			}
		}
		log.info("Moderación: {} resuelve la denuncia {} de la oferta {} con {}", administradorId, denunciaId,
				oferta.getId(), accion);
		return respuesta(denuncia);
	}

	/** Suspende la cuenta de una empresa: cierra sus sesiones, retira sus ofertas y resuelve sus denuncias. */
	@Transactional
	public void suspenderEmpresa(UUID administradorId, UUID empresaId) {
		Empresa empresa = empresaService.empresa(empresaId);
		Usuario usuario = empresa.getUsuario();
		if (usuario.isSuspendido()) {
			return;
		}
		usuario.suspender();
		sesiones.cerrarTodas(usuario.getId());
		int retiradas = ofertaService.retirarTodasDeLaEmpresa(empresaId);
		denuncias.findByOfertaEmpresaIdAndEstado(empresaId, EstadoDenuncia.PENDIENTE)
				.forEach(pendiente -> pendiente.resolver(EstadoDenuncia.ACEPTADA));
		avisos.cuentaSuspendida(empresa);
		log.info("Moderación: {} suspende la empresa {} y retira {} ofertas", administradorId, empresaId, retiradas);
	}

	private static DenunciaResponse respuesta(Denuncia denuncia) {
		Oferta oferta = denuncia.getOferta();
		Empresa empresa = oferta.getEmpresa();
		Usuario denunciante = denuncia.getDenunciante();
		return new DenunciaResponse(denuncia.getId(), denuncia.getMotivo(), denuncia.getDetalle(), denuncia.getEstado(),
				denuncia.getFechaCreacion(), denuncia.getFechaResolucion(),
				denunciante == null ? null : denunciante.getEmail(),
				new OfertaDenunciada(oferta.getId(), oferta.getTitulo(), oferta.getDescripcion(), oferta.getEstado(),
						empresa.getId(), empresa.getNombre(), empresa.getUsuario().getEmail(),
						empresa.getUsuario().isSuspendido()));
	}

}

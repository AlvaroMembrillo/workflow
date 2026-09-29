package com.alvaro.workflow.oferta;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.common.AccesoDenegadoException;
import com.alvaro.workflow.common.PeticionNoValidaException;
import com.alvaro.workflow.empresa.Empresa;
import com.alvaro.workflow.empresa.EmpresaService;
import com.alvaro.workflow.oferta.dto.OfertaRequest;
import com.alvaro.workflow.oferta.dto.OfertaResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OfertaService {

	private final OfertaRepository ofertas;
	private final EmpresaService empresaService;
	private final OfertaMapper ofertaMapper;

	@Transactional(readOnly = true)
	public Page<OfertaResponse> buscar(FiltroOfertas filtro, Pageable pageable) {
		return ofertas.findAll(filtro.comoEspecificacion(), pageable).map(ofertaMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public OfertaResponse obtener(UUID id) {
		return ofertaMapper.toResponse(oferta(id));
	}

	/** Una oferta de la empresa del usuario; 403 si es de otra empresa. */
	@Transactional(readOnly = true)
	public OfertaResponse obtenerDeLaEmpresaDelUsuario(UUID usuarioId, UUID ofertaId) {
		return ofertaMapper.toResponse(ofertaDeLaEmpresaDelUsuario(usuarioId, ofertaId));
	}

	/** Todas las ofertas de la empresa del usuario, también las cerradas. */
	@Transactional(readOnly = true)
	public Page<OfertaResponse> buscarDeLaEmpresaDelUsuario(UUID usuarioId, Pageable pageable) {
		Empresa empresa = empresaService.empresaDelUsuario(usuarioId);
		return ofertas.findByEmpresaId(empresa.getId(), pageable).map(ofertaMapper::toResponse);
	}

	@Transactional
	public OfertaResponse publicar(UUID usuarioId, OfertaRequest request) {
		validarSalario(request);
		Empresa empresa = empresaService.empresaDelUsuario(usuarioId);
		Oferta oferta = ofertas.save(new Oferta(empresa, ofertaMapper.toDatos(request)));
		return ofertaMapper.toResponse(oferta);
	}

	@Transactional
	public OfertaResponse actualizar(UUID usuarioId, UUID ofertaId, OfertaRequest request) {
		validarSalario(request);
		Oferta oferta = ofertaDeLaEmpresaDelUsuario(usuarioId, ofertaId);
		oferta.actualizar(ofertaMapper.toDatos(request));
		return ofertaMapper.toResponse(ofertas.saveAndFlush(oferta));
	}

	@Transactional
	public OfertaResponse cambiarEstado(UUID usuarioId, UUID ofertaId, EstadoOferta estado) {
		Oferta oferta = ofertaDeLaEmpresaDelUsuario(usuarioId, ofertaId);
		oferta.cambiarEstado(estado);
		return ofertaMapper.toResponse(ofertas.saveAndFlush(oferta));
	}

	/** Para otros módulos (por ejemplo, las candidaturas) que trabajan sobre una oferta. */
	@Transactional(readOnly = true)
	public Oferta oferta(UUID id) {
		return ofertas.findById(id).orElseThrow(() -> new OfertaNoEncontradaException(id));
	}

	/** Devuelve la oferta si pertenece a la empresa del usuario; si no, lanza {@link AccesoDenegadoException}. */
	@Transactional(readOnly = true)
	public Oferta ofertaDeLaEmpresaDelUsuario(UUID usuarioId, UUID ofertaId) {
		Oferta oferta = oferta(ofertaId);
		if (!oferta.esDeLaEmpresaDe(usuarioId)) {
			throw new AccesoDenegadoException("La oferta pertenece a otra empresa");
		}
		return oferta;
	}

	private static void validarSalario(OfertaRequest request) {
		if (request.salarioMinimo() > request.salarioMaximo()) {
			throw new PeticionNoValidaException("Rango salarial no válido",
					"El salario mínimo no puede ser mayor que el máximo");
		}
	}

}

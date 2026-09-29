package com.alvaro.workflow.candidatura;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.candidatura.dto.CandidaturaRecibidaResponse;
import com.alvaro.workflow.candidatura.dto.CandidaturaRequest;
import com.alvaro.workflow.candidatura.dto.MiCandidaturaResponse;
import com.alvaro.workflow.common.AccesoDenegadoException;
import com.alvaro.workflow.oferta.Oferta;
import com.alvaro.workflow.oferta.OfertaService;
import com.alvaro.workflow.usuario.Usuario;
import com.alvaro.workflow.usuario.UsuarioService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CandidaturaService {

	private final CandidaturaRepository candidaturas;
	private final OfertaService ofertaService;
	private final UsuarioService usuarioService;
	private final CandidaturaMapper candidaturaMapper;

	@Transactional
	public MiCandidaturaResponse inscribirse(UUID candidatoId, UUID ofertaId, CandidaturaRequest request) {
		Oferta oferta = ofertaService.oferta(ofertaId);
		if (!oferta.estaAbierta()) {
			throw new OfertaCerradaException();
		}
		// Si llegan dos peticiones a la vez, la restricción única de la tabla impide el duplicado
		// (GlobalExceptionHandler responde 409); esta comprobación da un mensaje más claro en el caso normal
		if (candidaturas.existsByOfertaIdAndCandidatoId(ofertaId, candidatoId)) {
			throw new CandidaturaDuplicadaException();
		}

		Usuario candidato = usuarioService.buscarPorId(candidatoId);
		Candidatura candidatura = candidaturas.save(new Candidatura(oferta, candidato, request.cartaPresentacion()));
		return candidaturaMapper.toMiCandidatura(candidatura);
	}

	@Transactional(readOnly = true)
	public Page<MiCandidaturaResponse> buscarDelCandidato(UUID candidatoId, Pageable pageable) {
		return candidaturas.findByCandidatoId(candidatoId, pageable).map(candidaturaMapper::toMiCandidatura);
	}

	/** Candidaturas de una oferta; solo puede verlas la empresa que la publicó. */
	@Transactional(readOnly = true)
	public Page<CandidaturaRecibidaResponse> buscarDeLaOferta(UUID usuarioEmpresaId, UUID ofertaId, Pageable pageable) {
		ofertaService.ofertaDeLaEmpresaDelUsuario(usuarioEmpresaId, ofertaId);
		return candidaturas.findByOfertaId(ofertaId, pageable).map(candidaturaMapper::toRecibida);
	}

	@Transactional
	public CandidaturaRecibidaResponse cambiarEstado(UUID usuarioEmpresaId, UUID candidaturaId, EstadoCandidatura estado) {
		Candidatura candidatura = candidaturas.findById(candidaturaId)
				.orElseThrow(() -> new CandidaturaNoEncontradaException(candidaturaId));
		if (!candidatura.getOferta().esDeLaEmpresaDe(usuarioEmpresaId)) {
			throw new AccesoDenegadoException("La candidatura es de una oferta de otra empresa");
		}

		candidatura.cambiarEstado(estado);
		return candidaturaMapper.toRecibida(candidaturas.saveAndFlush(candidatura));
	}

}

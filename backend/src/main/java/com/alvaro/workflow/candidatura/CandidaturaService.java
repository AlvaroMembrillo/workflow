package com.alvaro.workflow.candidatura;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.auth.EmailSinVerificarException;
import com.alvaro.workflow.candidatura.dto.CandidaturaRecibidaResponse;
import com.alvaro.workflow.candidatura.dto.CandidaturaRequest;
import com.alvaro.workflow.candidatura.dto.MiCandidaturaResponse;
import com.alvaro.workflow.candidatura.dto.ResumenCandidaturas;
import com.alvaro.workflow.common.AccesoDenegadoException;
import com.alvaro.workflow.common.PeticionNoValidaException;
import com.alvaro.workflow.common.RecursoNoEncontradoException;
import com.alvaro.workflow.cv.Curriculum;
import com.alvaro.workflow.cv.CurriculumResponse;
import com.alvaro.workflow.cv.CurriculumResumen;
import com.alvaro.workflow.cv.CurriculumService;
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
	private final AvisosDeCandidaturas avisos;
	private final CurriculumService curriculumService;

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
		// La empresa va a escribir a ese email: tiene que ser de quien se inscribe
		if (!candidato.isEmailVerificado()) {
			throw new EmailSinVerificarException("inscribirte en ofertas");
		}
		Candidatura candidatura = candidaturas.save(new Candidatura(oferta, candidato, request.cartaPresentacion()));
		avisos.recibida(candidatura);
		return candidaturaMapper.toMiCandidatura(candidatura);
	}

	@Transactional(readOnly = true)
	public Page<MiCandidaturaResponse> buscarDelCandidato(UUID candidatoId, Pageable pageable) {
		return candidaturas.findByCandidatoId(candidatoId, pageable).map(candidaturaMapper::toMiCandidatura);
	}

	/** La candidatura del candidato en una oferta, para saber en la ficha de la oferta si ya se inscribió. */
	@Transactional(readOnly = true)
	public MiCandidaturaResponse buscarDelCandidatoEnOferta(UUID candidatoId, UUID ofertaId) {
		return candidaturas.findByOfertaIdAndCandidatoId(ofertaId, candidatoId)
				.map(candidaturaMapper::toMiCandidatura)
				.orElseThrow(() -> new RecursoNoEncontradoException("Sin candidatura",
						"No te has inscrito en esta oferta"));
	}

	/** El candidato retira su candidatura mientras la empresa no haya decidido. */
	@Transactional
	public MiCandidaturaResponse retirar(UUID candidatoId, UUID candidaturaId) {
		Candidatura candidatura = candidatura(candidaturaId);
		if (!candidatura.esDe(candidatoId)) {
			throw new AccesoDenegadoException("La candidatura es de otra persona");
		}
		candidatura.cambiarEstado(EstadoCandidatura.RETIRADA);
		return candidaturaMapper.toMiCandidatura(candidaturas.saveAndFlush(candidatura));
	}

	/**
	 * Candidaturas de una oferta; solo puede verlas la empresa que la publicó.
	 *
	 * @param estados si no está vacío, solo se devuelven las candidaturas en esos estados
	 */
	@Transactional(readOnly = true)
	public Page<CandidaturaRecibidaResponse> buscarDeLaOferta(UUID usuarioEmpresaId, UUID ofertaId,
			Collection<EstadoCandidatura> estados, Pageable pageable) {
		ofertaService.ofertaDeLaEmpresaDelUsuario(usuarioEmpresaId, ofertaId);
		Page<Candidatura> pagina = estados == null || estados.isEmpty()
				? candidaturas.findByOfertaId(ofertaId, pageable)
				: candidaturas.findByOfertaIdAndEstadoIn(ofertaId, estados, pageable);
		Map<UUID, CurriculumResumen> curriculos = curriculumService
				.resumenesPorUsuario(pagina.map(candidatura -> candidatura.getCandidato().getId()).getContent());
		return pagina.map(candidatura -> candidaturaMapper.toRecibida(candidatura,
				CurriculumResponse.de(curriculos.get(candidatura.getCandidato().getId()))));
	}

	@Transactional
	public CandidaturaRecibidaResponse cambiarEstado(UUID usuarioEmpresaId, UUID candidaturaId, EstadoCandidatura estado) {
		if (estado == EstadoCandidatura.RETIRADA) {
			throw new PeticionNoValidaException("Estado no permitido", "Solo el candidato puede retirar su candidatura");
		}
		Candidatura candidatura = candidatura(candidaturaId);
		if (!candidatura.getOferta().esDeLaEmpresaDe(usuarioEmpresaId)) {
			throw new AccesoDenegadoException("La candidatura es de una oferta de otra empresa");
		}

		candidatura.cambiarEstado(estado);
		avisos.estadoCambiado(candidatura);
		return candidaturaMapper.toRecibida(candidaturas.saveAndFlush(candidatura),
				curriculumService.resumenSiExiste(candidatura.getCandidato().getId()).map(CurriculumResponse::de).orElse(null));
	}

	/**
	 * El currículum del candidato de una candidatura, para la empresa que publicó la oferta. Si el candidato
	 * retira su candidatura, la empresa deja de poder descargarlo.
	 */
	@Transactional(readOnly = true)
	public Curriculum curriculumDeLaCandidatura(UUID usuarioEmpresaId, UUID candidaturaId) {
		Candidatura candidatura = candidatura(candidaturaId);
		if (!candidatura.getOferta().esDeLaEmpresaDe(usuarioEmpresaId)) {
			throw new AccesoDenegadoException("La candidatura es de una oferta de otra empresa");
		}
		if (candidatura.getEstado() == EstadoCandidatura.RETIRADA) {
			throw new RecursoNoEncontradoException("Sin currículum", "El candidato ha retirado su candidatura");
		}
		return curriculumService.obtener(candidatura.getCandidato().getId());
	}

	/** Resumen de candidaturas por estado de cada oferta, calculado con una sola consulta. */
	@Transactional(readOnly = true)
	public Map<UUID, ResumenCandidaturas> resumenPorOferta(Collection<UUID> ofertaIds) {
		if (ofertaIds.isEmpty()) {
			return Map.of();
		}
		Map<UUID, Map<EstadoCandidatura, Long>> porOferta = new HashMap<>();
		for (CandidaturaRepository.RecuentoPorEstado fila : candidaturas.contarPorEstado(ofertaIds)) {
			porOferta.computeIfAbsent(fila.getOfertaId(), id -> new EnumMap<>(EstadoCandidatura.class))
					.put(fila.getEstado(), fila.getTotal());
		}
		return ofertaIds.stream().distinct().collect(Collectors.toMap(Function.identity(),
				id -> ResumenCandidaturas.de(porOferta.getOrDefault(id, Map.of()))));
	}

	private Candidatura candidatura(UUID id) {
		return candidaturas.findById(id).orElseThrow(() -> new CandidaturaNoEncontradaException(id));
	}

}

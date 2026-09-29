package com.alvaro.workflow.panel;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.candidatura.CandidaturaService;
import com.alvaro.workflow.candidatura.dto.ResumenCandidaturas;
import com.alvaro.workflow.oferta.OfertaService;
import com.alvaro.workflow.oferta.dto.OfertaResponse;
import com.alvaro.workflow.panel.dto.OfertaConCandidaturas;

import lombok.RequiredArgsConstructor;

/**
 * Datos del panel de la empresa. Combina ofertas y candidaturas para que esos dos módulos no dependan
 * el uno del otro.
 */
@Service
@RequiredArgsConstructor
public class PanelEmpresaService {

	private final OfertaService ofertaService;
	private final CandidaturaService candidaturaService;

	/** Ofertas de la empresa del usuario, también las cerradas, con el resumen de candidaturas de cada una. */
	@Transactional(readOnly = true)
	public Page<OfertaConCandidaturas> misOfertas(UUID usuarioId, Pageable pageable) {
		Page<OfertaResponse> ofertas = ofertaService.buscarDeLaEmpresaDelUsuario(usuarioId, pageable);
		Map<UUID, ResumenCandidaturas> resumen = candidaturaService
				.resumenPorOferta(ofertas.map(OfertaResponse::id).getContent());
		return ofertas.map(oferta -> new OfertaConCandidaturas(oferta,
				resumen.getOrDefault(oferta.id(), ResumenCandidaturas.VACIO)));
	}

}

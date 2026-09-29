package com.alvaro.workflow.panel.dto;

import com.alvaro.workflow.candidatura.dto.ResumenCandidaturas;
import com.alvaro.workflow.oferta.dto.OfertaResponse;

/** Una oferta de la empresa junto con cuántas candidaturas tiene en cada estado. */
public record OfertaConCandidaturas(OfertaResponse oferta, ResumenCandidaturas candidaturas) {
}

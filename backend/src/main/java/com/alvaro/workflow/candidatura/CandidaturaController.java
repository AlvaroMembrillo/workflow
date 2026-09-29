package com.alvaro.workflow.candidatura;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.workflow.auth.UsuarioActual;
import com.alvaro.workflow.candidatura.dto.CambioEstadoCandidaturaRequest;
import com.alvaro.workflow.candidatura.dto.CandidaturaRecibidaResponse;
import com.alvaro.workflow.candidatura.dto.CandidaturaRequest;
import com.alvaro.workflow.candidatura.dto.MiCandidaturaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Candidaturas")
public class CandidaturaController {

	private final CandidaturaService candidaturaService;

	@PostMapping("/ofertas/{ofertaId}/candidaturas")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('CANDIDATO')")
	@Operation(summary = "Inscribe al candidato en una oferta abierta")
	public MiCandidaturaResponse inscribirse(@UsuarioActual UUID usuarioId, @PathVariable UUID ofertaId,
			@Valid @RequestBody CandidaturaRequest request) {
		return candidaturaService.inscribirse(usuarioId, ofertaId, request);
	}

	@GetMapping("/candidaturas/me")
	@PreAuthorize("hasRole('CANDIDATO')")
	@Operation(summary = "Lista las candidaturas del candidato y en qué estado está cada una")
	public Page<MiCandidaturaResponse> misCandidaturas(@UsuarioActual UUID usuarioId,
			@ParameterObject @PageableDefault(sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
		return candidaturaService.buscarDelCandidato(usuarioId, pageable);
	}

	@GetMapping("/ofertas/{ofertaId}/candidaturas")
	@PreAuthorize("hasRole('EMPRESA')")
	@Operation(summary = "Lista las candidaturas recibidas en una oferta de la empresa del usuario")
	public Page<CandidaturaRecibidaResponse> candidaturasDeLaOferta(@UsuarioActual UUID usuarioId,
			@PathVariable UUID ofertaId,
			@ParameterObject @PageableDefault(sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
		return candidaturaService.buscarDeLaOferta(usuarioId, ofertaId, pageable);
	}

	@PatchMapping("/candidaturas/{id}/estado")
	@PreAuthorize("hasRole('EMPRESA')")
	@Operation(summary = "Cambia el estado de una candidatura recibida",
			description = "PENDIENTE → EN_REVISION → ACEPTADA o RECHAZADA. Una decisión final no se puede cambiar.")
	public CandidaturaRecibidaResponse cambiarEstado(@UsuarioActual UUID usuarioId, @PathVariable UUID id,
			@Valid @RequestBody CambioEstadoCandidaturaRequest request) {
		return candidaturaService.cambiarEstado(usuarioId, id, request.estado());
	}

}

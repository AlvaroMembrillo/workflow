package com.alvaro.workflow.oferta;

import java.net.URI;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.alvaro.workflow.auth.UsuarioActual;
import com.alvaro.workflow.oferta.dto.CambioEstadoOfertaRequest;
import com.alvaro.workflow.oferta.dto.OfertaRequest;
import com.alvaro.workflow.oferta.dto.OfertaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ofertas")
@RequiredArgsConstructor
@Tag(name = "Ofertas")
public class OfertaController {

	private final OfertaService ofertaService;

	@GetMapping
	@SecurityRequirements
	@Operation(summary = "Busca ofertas abiertas", description = "Paginado. Por defecto, las más recientes primero.")
	public Page<OfertaResponse> buscar(@ParameterObject FiltroOfertas filtro,
			@ParameterObject @PageableDefault(sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
		return ofertaService.buscar(filtro, pageable);
	}

	@GetMapping("/{id}")
	@SecurityRequirements
	@Operation(summary = "Devuelve una oferta")
	public OfertaResponse obtener(@PathVariable UUID id) {
		return ofertaService.obtener(id);
	}

	@PostMapping
	@PreAuthorize("hasRole('EMPRESA')")
	@Operation(summary = "Publica una oferta de la empresa del usuario")
	public ResponseEntity<OfertaResponse> publicar(@UsuarioActual UUID usuarioId, @Valid @RequestBody OfertaRequest request) {
		OfertaResponse oferta = ofertaService.publicar(usuarioId, request);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(oferta.id()).toUri();
		return ResponseEntity.created(ubicacion).body(oferta);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('EMPRESA')")
	@Operation(summary = "Modifica una oferta de la empresa del usuario")
	public OfertaResponse actualizar(@UsuarioActual UUID usuarioId, @PathVariable UUID id,
			@Valid @RequestBody OfertaRequest request) {
		return ofertaService.actualizar(usuarioId, id, request);
	}

	@PatchMapping("/{id}/estado")
	@PreAuthorize("hasRole('EMPRESA')")
	@Operation(summary = "Abre o cierra una oferta de la empresa del usuario")
	public OfertaResponse cambiarEstado(@UsuarioActual UUID usuarioId, @PathVariable UUID id,
			@Valid @RequestBody CambioEstadoOfertaRequest request) {
		return ofertaService.cambiarEstado(usuarioId, id, request.estado());
	}

}

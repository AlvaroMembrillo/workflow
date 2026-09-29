package com.alvaro.workflow.empresa;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.workflow.auth.UsuarioActual;
import com.alvaro.workflow.empresa.dto.EmpresaRequest;
import com.alvaro.workflow.empresa.dto.EmpresaResponse;
import com.alvaro.workflow.oferta.OfertaService;
import com.alvaro.workflow.oferta.dto.OfertaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@Tag(name = "Empresas")
public class EmpresaController {

	private final EmpresaService empresaService;
	private final OfertaService ofertaService;

	@GetMapping("/{id}")
	@SecurityRequirements
	@Operation(summary = "Devuelve el perfil público de una empresa")
	public EmpresaResponse obtener(@PathVariable UUID id) {
		return empresaService.obtener(id);
	}

	@GetMapping("/me")
	@PreAuthorize("hasRole('EMPRESA')")
	@Operation(summary = "Devuelve el perfil de la empresa del usuario")
	public EmpresaResponse miEmpresa(@UsuarioActual UUID usuarioId) {
		return empresaService.obtenerDelUsuario(usuarioId);
	}

	@PutMapping("/me")
	@PreAuthorize("hasRole('EMPRESA')")
	@Operation(summary = "Modifica el perfil de la empresa del usuario")
	public EmpresaResponse actualizarMiEmpresa(@UsuarioActual UUID usuarioId, @Valid @RequestBody EmpresaRequest request) {
		return empresaService.actualizarDelUsuario(usuarioId, request);
	}

	@GetMapping("/me/ofertas")
	@PreAuthorize("hasRole('EMPRESA')")
	@Operation(summary = "Lista las ofertas de la empresa del usuario, también las cerradas")
	public Page<OfertaResponse> misOfertas(@UsuarioActual UUID usuarioId,
			@ParameterObject @PageableDefault(sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
		return ofertaService.buscarDeLaEmpresaDelUsuario(usuarioId, pageable);
	}

}

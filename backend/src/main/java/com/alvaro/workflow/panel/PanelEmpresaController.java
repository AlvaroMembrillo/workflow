package com.alvaro.workflow.panel;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.workflow.auth.UsuarioActual;
import com.alvaro.workflow.panel.dto.OfertaConCandidaturas;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/empresas/me")
@RequiredArgsConstructor
@Tag(name = "Empresas")
public class PanelEmpresaController {

	private final PanelEmpresaService panelEmpresaService;

	@GetMapping("/ofertas")
	@PreAuthorize("hasRole('EMPRESA')")
	@Operation(summary = "Lista las ofertas de la empresa del usuario, también las cerradas",
			description = "Cada oferta incluye cuántas candidaturas tiene en cada estado.")
	public Page<OfertaConCandidaturas> misOfertas(@UsuarioActual UUID usuarioId,
			@ParameterObject @PageableDefault(sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
		return panelEmpresaService.misOfertas(usuarioId, pageable);
	}

}

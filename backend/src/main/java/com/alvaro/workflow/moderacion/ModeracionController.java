package com.alvaro.workflow.moderacion;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.workflow.auth.UsuarioActual;
import com.alvaro.workflow.moderacion.dto.DenunciaResponse;
import com.alvaro.workflow.moderacion.dto.ResolucionRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Moderación")
public class ModeracionController {

	private final DenunciaService denunciaService;

	@GetMapping("/denuncias")
	@Operation(summary = "Lista las denuncias, por defecto las pendientes por orden de llegada")
	public Page<DenunciaResponse> denuncias(
			@RequestParam(defaultValue = "PENDIENTE") EstadoDenuncia estado,
			@ParameterObject @PageableDefault(sort = "fechaCreacion", direction = Sort.Direction.ASC) Pageable pageable) {
		return denunciaService.buscar(estado, pageable);
	}

	@PostMapping("/denuncias/{id}/resolucion")
	@Operation(summary = "Resuelve una denuncia: retira la oferta o la desestima",
			description = "Retirar la oferta resuelve también las demás denuncias de esa oferta y avisa a la empresa.")
	public DenunciaResponse resolver(@UsuarioActual UUID administradorId, @PathVariable UUID id,
			@Valid @RequestBody ResolucionRequest request) {
		return denunciaService.resolver(administradorId, id, request.accion());
	}

	@PostMapping("/empresas/{id}/suspension")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Suspende la cuenta de una empresa",
			description = "Cierra sus sesiones, retira todas sus ofertas y le avisa por correo.")
	public void suspenderEmpresa(@UsuarioActual UUID administradorId, @PathVariable UUID id) {
		denunciaService.suspenderEmpresa(administradorId, id);
	}

}

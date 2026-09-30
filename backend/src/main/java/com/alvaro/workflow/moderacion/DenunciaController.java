package com.alvaro.workflow.moderacion;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.workflow.auth.UsuarioActual;
import com.alvaro.workflow.moderacion.dto.DenunciaRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ofertas/{ofertaId}/denuncias")
@RequiredArgsConstructor
@Tag(name = "Moderación")
public class DenunciaController {

	private final DenunciaService denunciaService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Denuncia una oferta que incumple las normas",
			description = "Cualquier usuario con sesión, una vez por oferta. La revisa un administrador.")
	public void denunciar(@UsuarioActual UUID usuarioId, @PathVariable UUID ofertaId,
			@Valid @RequestBody DenunciaRequest request) {
		denunciaService.denunciar(usuarioId, ofertaId, request);
	}

}

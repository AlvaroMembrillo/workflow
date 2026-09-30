package com.alvaro.workflow.usuario;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.workflow.auth.PasswordService;
import com.alvaro.workflow.auth.UsuarioActual;
import com.alvaro.workflow.auth.dto.CambioDePasswordRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios")
public class UsuarioController {

	private final UsuarioService usuarioService;
	private final UsuarioMapper usuarioMapper;
	private final PasswordService passwordService;

	@GetMapping("/me")
	@Operation(summary = "Devuelve los datos del usuario autenticado")
	public UsuarioResponse me(@UsuarioActual UUID usuarioId) {
		return usuarioMapper.toResponse(usuarioService.buscarPorId(usuarioId));
	}

	@PatchMapping("/me")
	@Operation(summary = "Cambia las preferencias del usuario autenticado")
	public UsuarioResponse cambiarPreferencias(@UsuarioActual UUID usuarioId,
			@Valid @RequestBody PreferenciasRequest request) {
		return usuarioMapper.toResponse(usuarioService.cambiarPreferencias(usuarioId, request.avisosPorCorreo()));
	}

	@PostMapping("/me/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Cambia la contraseña del usuario autenticado, que debe indicar la actual")
	public void cambiarPassword(@UsuarioActual UUID usuarioId, @Valid @RequestBody CambioDePasswordRequest request) {
		passwordService.cambiar(usuarioId, request.passwordActual(), request.passwordNueva());
	}

}

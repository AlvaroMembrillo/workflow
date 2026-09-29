package com.alvaro.workflow.usuario;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios")
public class UsuarioController {

	private final UsuarioService usuarioService;
	private final UsuarioMapper usuarioMapper;

	@GetMapping("/me")
	@Operation(summary = "Devuelve los datos del usuario autenticado")
	public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
		// El "sub" del token es el id del usuario (ver TokenService)
		return usuarioMapper.toResponse(usuarioService.buscarPorId(UUID.fromString(jwt.getSubject())));
	}

}

package com.alvaro.workflow.usuario;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.workflow.auth.UsuarioActual;

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
	public UsuarioResponse me(@UsuarioActual UUID usuarioId) {
		return usuarioMapper.toResponse(usuarioService.buscarPorId(usuarioId));
	}

}

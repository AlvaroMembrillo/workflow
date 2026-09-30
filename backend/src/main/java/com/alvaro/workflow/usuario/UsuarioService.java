package com.alvaro.workflow.usuario;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

	private final UsuarioRepository usuarios;

	@Transactional(readOnly = true)
	public Usuario buscarPorId(UUID id) {
		return usuarios.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
	}

	@Transactional
	public Usuario cambiarPreferencias(UUID id, boolean avisosPorCorreo) {
		Usuario usuario = buscarPorId(id);
		usuario.cambiarAvisosPorCorreo(avisosPorCorreo);
		return usuario;
	}

}

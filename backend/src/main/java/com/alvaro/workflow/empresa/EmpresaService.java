package com.alvaro.workflow.empresa;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.empresa.dto.EmpresaRequest;
import com.alvaro.workflow.empresa.dto.EmpresaResponse;
import com.alvaro.workflow.usuario.Usuario;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmpresaService {

	private final EmpresaRepository empresas;
	private final EmpresaMapper empresaMapper;

	/** Crea el perfil de una cuenta EMPRESA recién registrada, usando como nombre el del registro. */
	@Transactional
	public void crearPerfil(Usuario usuario) {
		empresas.save(new Empresa(usuario, usuario.getNombre()));
	}

	@Transactional(readOnly = true)
	public EmpresaResponse obtener(UUID id) {
		return empresas.findById(id)
				.map(empresaMapper::toResponse)
				.orElseThrow(() -> new EmpresaNoEncontradaException("No existe ninguna empresa con id " + id));
	}

	@Transactional(readOnly = true)
	public EmpresaResponse obtenerDelUsuario(UUID usuarioId) {
		return empresaMapper.toResponse(empresaDelUsuario(usuarioId));
	}

	@Transactional
	public EmpresaResponse actualizarDelUsuario(UUID usuarioId, EmpresaRequest request) {
		Empresa empresa = empresaDelUsuario(usuarioId);
		empresa.actualizarPerfil(request.nombre().strip(), request.descripcion(), request.sitioWeb(), request.ubicacion());
		return empresaMapper.toResponse(empresa);
	}

	/** Para otros módulos (por ejemplo, las ofertas) que necesitan la empresa del usuario autenticado. */
	@Transactional(readOnly = true)
	public Empresa empresaDelUsuario(UUID usuarioId) {
		return empresas.findByUsuarioId(usuarioId)
				.orElseThrow(() -> new EmpresaNoEncontradaException("El usuario no tiene un perfil de empresa"));
	}

}

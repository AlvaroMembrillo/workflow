package com.alvaro.workflow.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.alvaro.workflow.usuario.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarios;

	@Override
	public UserDetails loadUserByUsername(String email) {
		return usuarios.findByEmail(email)
				.map(UsuarioAutenticado::new)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
	}

}

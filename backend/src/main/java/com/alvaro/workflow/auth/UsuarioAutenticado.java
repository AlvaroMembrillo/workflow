package com.alvaro.workflow.auth;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.alvaro.workflow.usuario.Usuario;

/**
 * Adapta {@link Usuario} a lo que espera Spring Security durante el login.
 * Solo se usa para comprobar credenciales: las peticiones posteriores se autentican con el JWT.
 */
record UsuarioAutenticado(Usuario usuario) implements UserDetails {

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
	}

	@Override
	public String getPassword() {
		return usuario.getPasswordHash();
	}

	@Override
	public String getUsername() {
		return usuario.getEmail();
	}

}

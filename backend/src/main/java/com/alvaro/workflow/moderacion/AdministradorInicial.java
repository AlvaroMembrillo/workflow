package com.alvaro.workflow.moderacion;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.usuario.Rol;
import com.alvaro.workflow.usuario.Usuario;
import com.alvaro.workflow.usuario.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Crea la cuenta de administrador indicada en la configuración la primera vez que arranca la aplicación.
 * Si la cuenta ya existe no la toca: la contraseña se cambia después desde la web, no desde la configuración.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class AdministradorInicial implements ApplicationRunner {

	private static final int LONGITUD_MINIMA_PASSWORD = 12;

	private final AdminProperties properties;
	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!properties.configurada()) {
			return;
		}
		String email = Usuario.normalizarEmail(properties.email());
		if (usuarios.existsByEmail(email)) {
			return;
		}
		if (properties.password() == null || properties.password().length() < LONGITUD_MINIMA_PASSWORD) {
			throw new IllegalStateException("ADMIN_PASSWORD tiene que tener al menos "
					+ LONGITUD_MINIMA_PASSWORD + " caracteres para crear la cuenta de administrador");
		}
		Usuario administrador = new Usuario(email, passwordEncoder.encode(properties.password()), "Moderación", Rol.ADMIN);
		administrador.verificarEmail();
		usuarios.save(administrador);
		log.info("Creada la cuenta de administrador indicada en la configuración");
	}

}

package com.alvaro.workflow.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.auth.dto.LoginRequest;
import com.alvaro.workflow.auth.dto.RegistroRequest;
import com.alvaro.workflow.auth.dto.TokenResponse;
import com.alvaro.workflow.empresa.EmpresaService;
import com.alvaro.workflow.usuario.Rol;
import com.alvaro.workflow.usuario.Usuario;
import com.alvaro.workflow.usuario.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final TokenService tokenService;
	private final EmpresaService empresaService;
	private final VerificacionEmailService verificacionEmail;

	@Transactional
	public TokenResponse registrar(RegistroRequest request) {
		Usuario usuario = crearCuenta(request);
		verificacionEmail.enviarEnlace(usuario);
		return tokenService.generar(usuario);
	}

	/** Para los datos de ejemplo: crea la cuenta con el email ya verificado y sin enviar ningún correo. */
	@Transactional
	public Usuario crearCuentaVerificada(RegistroRequest request) {
		Usuario usuario = crearCuenta(request);
		usuario.verificarEmail();
		return usuario;
	}

	private Usuario crearCuenta(RegistroRequest request) {
		// Las cuentas de administrador no se crean desde el registro público
		if (request.rol() == Rol.ADMIN) {
			throw new RolNoPermitidoException(request.rol());
		}

		String email = Usuario.normalizarEmail(request.email());
		if (usuarios.existsByEmail(email)) {
			throw new EmailYaRegistradoException(email);
		}

		Usuario usuario = usuarios.save(new Usuario(
				email, passwordEncoder.encode(request.password()), request.nombre().strip(), request.rol()));
		if (usuario.getRol() == Rol.EMPRESA) {
			empresaService.crearPerfil(usuario);
		}
		return usuario;
	}

	public TokenResponse login(LoginRequest request) {
		// DaoAuthenticationProvider responde igual (y tarda lo mismo) si el email no existe
		// o si la contraseña es incorrecta, así no se puede averiguar qué emails están registrados
		Authentication autenticacion = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(Usuario.normalizarEmail(request.email()), request.password()));

		UsuarioAutenticado usuario = (UsuarioAutenticado) autenticacion.getPrincipal();
		return tokenService.generar(usuario.usuario());
	}

}

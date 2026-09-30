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
import com.alvaro.workflow.correo.CorreoProperties;
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
	private final SesionService sesiones;
	private final CorreoProperties correo;

	/**
	 * Lo que recibe el cliente al abrir o renovar una sesión.
	 *
	 * @param tokenDeRefresco el token para la cookie de sesión, o null si la cookie actual sigue valiendo
	 */
	public record Acceso(TokenResponse token, String tokenDeRefresco) {
	}

	@Transactional
	public Acceso registrar(RegistroRequest request) {
		Usuario usuario = crearCuenta(request);
		verificacionEmail.enviarEnlace(usuario);
		return abrirSesion(usuario);
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

		Usuario usuario = new Usuario(
				email, passwordEncoder.encode(request.password()), request.nombre().strip(), request.rol());
		usuario.aceptarCondiciones();
		usuario = usuarios.save(usuario);
		if (usuario.getRol() == Rol.EMPRESA) {
			empresaService.crearPerfil(usuario);
		}
		return usuario;
	}

	@Transactional
	public Acceso login(LoginRequest request) {
		// DaoAuthenticationProvider responde igual (y tarda lo mismo) si el email no existe
		// o si la contraseña es incorrecta, así no se puede averiguar qué emails están registrados
		Authentication autenticacion = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(Usuario.normalizarEmail(request.email()), request.password()));

		Usuario usuario = ((UsuarioAutenticado) autenticacion.getPrincipal()).usuario();
		// Se comprueba después de la contraseña: así solo el dueño de la cuenta se entera de que está suspendida
		if (usuario.isSuspendido()) {
			throw new CuentaSuspendidaException(correo.contacto());
		}
		return abrirSesion(usuario);
	}

	/** Canjea el token de refresco de la cookie por un token de acceso nuevo. */
	@Transactional(noRollbackFor = SesionNoValidaException.class)
	public Acceso renovar(String tokenDeRefresco) {
		SesionService.Renovacion renovacion = sesiones.renovar(tokenDeRefresco);
		return new Acceso(tokenService.generar(renovacion.usuario()), renovacion.tokenDeRefresco());
	}

	private Acceso abrirSesion(Usuario usuario) {
		return new Acceso(tokenService.generar(usuario), sesiones.abrir(usuario));
	}

}

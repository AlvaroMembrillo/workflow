package com.alvaro.workflow.auth;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.workflow.auth.dto.EnlaceRequest;
import com.alvaro.workflow.auth.dto.LoginRequest;
import com.alvaro.workflow.auth.dto.RecuperacionRequest;
import com.alvaro.workflow.auth.dto.RegistroRequest;
import com.alvaro.workflow.auth.dto.RestablecimientoRequest;
import com.alvaro.workflow.auth.dto.TokenResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación")
@SecurityRequirements // endpoints públicos: no requieren token
public class AuthController {

	private final AuthService authService;
	private final VerificacionEmailService verificacionEmail;
	private final PasswordService passwordService;

	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Registra un candidato o una empresa y devuelve su token de acceso")
	public TokenResponse registrar(@Valid @RequestBody RegistroRequest request) {
		return authService.registrar(request);
	}

	@PostMapping("/login")
	@Operation(summary = "Inicia sesión y devuelve un token de acceso")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/verificacion")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Verifica el email con el token del enlace enviado por correo")
	public void verificarEmail(@Valid @RequestBody EnlaceRequest request) {
		verificacionEmail.verificar(request.token());
	}

	@PostMapping("/verificacion/reenvio")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Vuelve a enviar el enlace de verificación al usuario autenticado")
	public void reenviarVerificacion(@UsuarioActual UUID usuarioId) {
		verificacionEmail.reenviar(usuarioId);
	}

	@PostMapping("/recuperacion")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Envía un enlace para cambiar la contraseña",
			description = "Responde 204 exista o no una cuenta con ese email, para no revelar qué emails están registrados.")
	public void recuperarPassword(@Valid @RequestBody RecuperacionRequest request) {
		passwordService.solicitarCambio(request.email());
	}

	@PostMapping("/restablecimiento")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Cambia la contraseña con el token del enlace enviado por correo")
	public void restablecerPassword(@Valid @RequestBody RestablecimientoRequest request) {
		passwordService.restablecer(request.token(), request.password());
	}

}

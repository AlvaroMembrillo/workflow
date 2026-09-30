package com.alvaro.workflow.auth;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
	private final SesionService sesiones;
	private final CookieDeSesion cookie;

	@PostMapping("/registro")
	@Operation(summary = "Registra un candidato o una empresa y abre su sesión",
			description = "Devuelve un token de acceso y deja el token de refresco en una cookie HttpOnly.")
	public ResponseEntity<TokenResponse> registrar(@Valid @RequestBody RegistroRequest request) {
		return conSesion(HttpStatus.CREATED, authService.registrar(request));
	}

	@PostMapping("/login")
	@Operation(summary = "Inicia sesión",
			description = "Devuelve un token de acceso y deja el token de refresco en una cookie HttpOnly.")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return conSesion(HttpStatus.OK, authService.login(request));
	}

	@PostMapping("/refresco")
	@Operation(summary = "Devuelve otro token de acceso a cambio del token de refresco de la cookie",
			description = "El token de refresco solo vale una vez: la respuesta trae el siguiente en la cookie. "
					+ "401 si la sesión ha caducado o se ha cerrado.")
	public ResponseEntity<TokenResponse> refrescar(
			@CookieValue(name = CookieDeSesion.NOMBRE, required = false) String tokenDeRefresco) {
		if (tokenDeRefresco == null) {
			throw new SesionNoValidaException();
		}
		return conSesion(HttpStatus.OK, authService.renovar(tokenDeRefresco));
	}

	@PostMapping("/salida")
	@Operation(summary = "Cierra la sesión: invalida el token de refresco y borra la cookie")
	public ResponseEntity<Void> salir(@CookieValue(name = CookieDeSesion.NOMBRE, required = false) String tokenDeRefresco) {
		if (tokenDeRefresco != null) {
			sesiones.cerrar(tokenDeRefresco);
		}
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.borrada().toString()).build();
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

	/** La sesión de la cookie ya no vale: además de responder 401, se le pide al navegador que la borre. */
	@ExceptionHandler(SesionNoValidaException.class)
	ResponseEntity<ProblemDetail> sesionNoValida(SesionNoValidaException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
		problema.setTitle(ex.getTitulo());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.header(HttpHeaders.SET_COOKIE, cookie.borrada().toString())
				.body(problema);
	}

	private ResponseEntity<TokenResponse> conSesion(HttpStatus estado, AuthService.Acceso acceso) {
		ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(estado);
		if (acceso.tokenDeRefresco() != null) {
			respuesta.header(HttpHeaders.SET_COOKIE, cookie.con(acceso.tokenDeRefresco()).toString());
		}
		return respuesta.body(acceso.token());
	}

}

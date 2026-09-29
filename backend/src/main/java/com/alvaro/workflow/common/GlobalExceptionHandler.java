package com.alvaro.workflow.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.alvaro.workflow.auth.EmailYaRegistradoException;
import com.alvaro.workflow.auth.RolNoPermitidoException;
import com.alvaro.workflow.usuario.UsuarioNoEncontradoException;

/**
 * Traduce las excepciones a respuestas de error con formato Problem Details (RFC 9457).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(EmailYaRegistradoException.class)
	ProblemDetail emailYaRegistrado(EmailYaRegistradoException ex) {
		return problema(HttpStatus.CONFLICT, "Email ya registrado", ex.getMessage());
	}

	@ExceptionHandler(RolNoPermitidoException.class)
	ProblemDetail rolNoPermitido(RolNoPermitidoException ex) {
		return problema(HttpStatus.BAD_REQUEST, "Rol no permitido", ex.getMessage());
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail credencialesIncorrectas() {
		// Mismo mensaje para email inexistente y contraseña incorrecta
		return problema(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas", "El email o la contraseña no son correctos");
	}

	@ExceptionHandler(UsuarioNoEncontradoException.class)
	ProblemDetail usuarioNoEncontrado(UsuarioNoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, "Usuario no encontrado", ex.getMessage());
	}

	/** Añade a la respuesta el error de cada campo, para que el frontend pueda mostrarlo junto al campo. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errores = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));

		ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Datos no válidos", "La petición contiene campos no válidos");
		problema.setProperty("errores", errores);
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

	private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
		problema.setTitle(titulo);
		return problema;
	}

}

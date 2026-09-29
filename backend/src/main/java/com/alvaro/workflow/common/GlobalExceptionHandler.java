package com.alvaro.workflow.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce las excepciones a respuestas de error con formato Problem Details (RFC 9457).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(RecursoNoEncontradoException.class)
	ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, ex);
	}

	@ExceptionHandler(ConflictoException.class)
	ProblemDetail conflicto(ConflictoException ex) {
		return problema(HttpStatus.CONFLICT, ex);
	}

	@ExceptionHandler(PeticionNoValidaException.class)
	ProblemDetail peticionNoValida(PeticionNoValidaException ex) {
		return problema(HttpStatus.BAD_REQUEST, ex);
	}

	@ExceptionHandler(AccesoDenegadoException.class)
	ProblemDetail accesoDenegado(AccesoDenegadoException ex) {
		return problema(HttpStatus.FORBIDDEN, ex);
	}

	/**
	 * Una restricción de la base de datos (por ejemplo, un índice único) ha rechazado el cambio. Pasa cuando dos
	 * peticiones iguales llegan a la vez y ambas superan las comprobaciones previas del servicio.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail violacionDeIntegridad() {
		return problema(HttpStatus.CONFLICT, "Conflicto", "La operación entra en conflicto con los datos existentes");
	}

	/** Un {@code @PreAuthorize} ha rechazado la petición: el usuario no tiene el rol necesario. */
	@ExceptionHandler(AuthorizationDeniedException.class)
	ProblemDetail rolInsuficiente() {
		return problema(HttpStatus.FORBIDDEN, "Acceso denegado", "Tu tipo de cuenta no puede realizar esta acción");
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail credencialesIncorrectas() {
		// Mismo mensaje para email inexistente y contraseña incorrecta
		return problema(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas", "El email o la contraseña no son correctos");
	}

	/** Se pide ordenar por un campo que no existe, por ejemplo {@code ?sort=noExiste}. */
	@ExceptionHandler(PropertyReferenceException.class)
	ProblemDetail ordenNoValido(PropertyReferenceException ex) {
		return problema(HttpStatus.BAD_REQUEST, "Orden no válido", "No se puede ordenar por '" + ex.getPropertyName() + "'");
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

	private static ProblemDetail problema(HttpStatus estado, ErrorDeNegocioException ex) {
		return problema(estado, ex.getTitulo(), ex.getMessage());
	}

	private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
		problema.setTitle(titulo);
		return problema;
	}

}

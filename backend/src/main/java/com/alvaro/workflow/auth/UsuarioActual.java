package com.alvaro.workflow.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Inyecta en un parámetro {@code UUID} el id del usuario autenticado, que es el "sub" de su JWT.
 * Solo se puede usar en endpoints que exigen token.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal(expression = "T(java.util.UUID).fromString(subject)")
public @interface UsuarioActual {
}

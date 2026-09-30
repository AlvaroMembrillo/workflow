package com.alvaro.workflow.usuario;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Cuenta de acceso a la aplicación. Los datos de perfil de candidatos y empresas
 * vivirán en sus propias entidades.
 */
@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(nullable = false)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Rol rol;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private Instant fechaCreacion;

	/** Cuándo confirmó que el email es suyo; null mientras no lo haya hecho. */
	@Column(name = "email_verificado_en")
	private Instant emailVerificadoEn;

	/** Si quiere recibir avisos por correo (nueva candidatura, cambio de estado). */
	@Column(name = "avisos_por_correo", nullable = false)
	private boolean avisosPorCorreo;

	public Usuario(String email, String passwordHash, String nombre, Rol rol) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.nombre = nombre;
		this.rol = rol;
		this.fechaCreacion = Instant.now();
		this.avisosPorCorreo = true;
	}

	/** Los emails se guardan sin espacios y en minúsculas, para que no haya dos cuentas con el mismo. */
	public static String normalizarEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

	public boolean isEmailVerificado() {
		return emailVerificadoEn != null;
	}

	/** Marca el email como verificado. Si ya lo estaba, conserva la fecha original. */
	public void verificarEmail() {
		if (emailVerificadoEn == null) {
			emailVerificadoEn = Instant.now();
		}
	}

	public void cambiarPassword(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public void cambiarAvisosPorCorreo(boolean avisosPorCorreo) {
		this.avisosPorCorreo = avisosPorCorreo;
	}

	/** Solo se envían avisos a direcciones verificadas, para no escribir a quien no ha creado la cuenta. */
	public boolean recibeAvisos() {
		return avisosPorCorreo && isEmailVerificado();
	}

}

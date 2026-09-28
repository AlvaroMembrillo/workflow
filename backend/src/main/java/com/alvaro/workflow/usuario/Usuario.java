package com.alvaro.workflow.usuario;

import java.time.Instant;
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

	public Usuario(String email, String passwordHash, String nombre, Rol rol) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.nombre = nombre;
		this.rol = rol;
		this.fechaCreacion = Instant.now();
	}

}

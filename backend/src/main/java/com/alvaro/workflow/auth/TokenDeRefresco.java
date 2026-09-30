package com.alvaro.workflow.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.alvaro.workflow.usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Token de refresco de una sesión. Viaja en una cookie HttpOnly y aquí solo se guarda su hash.
 * Todos los tokens que salen de un mismo inicio de sesión comparten {@code familia}.
 */
@Entity
@Table(name = "tokens_de_refresco")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TokenDeRefresco {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(nullable = false, updatable = false)
	private UUID familia;

	@Column(nullable = false, unique = true)
	private String hash;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private Instant fechaCreacion;

	@Column(name = "fecha_caducidad", nullable = false, updatable = false)
	private Instant fechaCaducidad;

	@Column(name = "fecha_uso")
	private Instant fechaUso;

	@Column(name = "fecha_revocacion")
	private Instant fechaRevocacion;

	TokenDeRefresco(Usuario usuario, UUID familia, String hash, Duration duracion) {
		this.usuario = usuario;
		this.familia = familia;
		this.hash = hash;
		this.fechaCreacion = Instant.now();
		this.fechaCaducidad = this.fechaCreacion.plus(duracion);
	}

	/** Ni revocado ni caducado (aunque puede estar ya canjeado por el siguiente). */
	boolean sigueEnPie(Instant ahora) {
		return fechaRevocacion == null && fechaCaducidad.isAfter(ahora);
	}

}

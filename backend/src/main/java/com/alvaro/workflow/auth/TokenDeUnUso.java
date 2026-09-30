package com.alvaro.workflow.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.alvaro.workflow.usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Enlace de un solo uso enviado por correo. El token viaja en el enlace y aquí solo se guarda su hash.
 */
@Entity
@Table(name = "tokens_de_un_uso")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TokenDeUnUso {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoDeToken tipo;

	@Column(nullable = false, unique = true)
	private String hash;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private Instant fechaCreacion;

	@Column(name = "fecha_caducidad", nullable = false, updatable = false)
	private Instant fechaCaducidad;

	@Column(name = "fecha_uso")
	private Instant fechaUso;

	TokenDeUnUso(Usuario usuario, TipoDeToken tipo, String hash, Duration validez) {
		this.usuario = usuario;
		this.tipo = tipo;
		this.hash = hash;
		this.fechaCreacion = Instant.now();
		this.fechaCaducidad = this.fechaCreacion.plus(validez);
	}

}

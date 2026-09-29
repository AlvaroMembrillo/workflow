package com.alvaro.workflow.empresa;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Perfil público de una cuenta con rol EMPRESA. Se crea al registrarse.
 */
@Entity
@Table(name = "empresas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Empresa {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, unique = true)
	private Usuario usuario;

	@Column(nullable = false)
	private String nombre;

	@Column(columnDefinition = "text")
	private String descripcion;

	@Column(name = "sitio_web")
	private String sitioWeb;

	private String ubicacion;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private Instant fechaCreacion;

	public Empresa(Usuario usuario, String nombre) {
		this.usuario = usuario;
		this.nombre = nombre;
		this.fechaCreacion = Instant.now();
	}

	public void actualizarPerfil(String nombre, String descripcion, String sitioWeb, String ubicacion) {
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.sitioWeb = sitioWeb;
		this.ubicacion = ubicacion;
	}

	public boolean perteneceA(UUID usuarioId) {
		return usuario.getId().equals(usuarioId);
	}

}

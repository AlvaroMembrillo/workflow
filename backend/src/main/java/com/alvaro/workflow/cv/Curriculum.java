package com.alvaro.workflow.cv;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * El currículum de un candidato, en PDF. Cada candidato tiene como mucho uno: al subir otro se sustituye.
 * Los listados usan {@link CurriculumResumen} para no traer el contenido de la base de datos.
 */
@Entity
@Table(name = "curriculos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Curriculum {

	@Id
	@Column(name = "usuario_id")
	private UUID usuarioId;

	@Column(name = "nombre_fichero", nullable = false)
	private String nombreFichero;

	@Column(nullable = false)
	private int tamano;

	@Column(nullable = false)
	private byte[] contenido;

	@Column(name = "fecha_subida", nullable = false)
	private Instant fechaSubida;

	Curriculum(UUID usuarioId, String nombreFichero, byte[] contenido) {
		this.usuarioId = usuarioId;
		sustituir(nombreFichero, contenido);
	}

	void sustituir(String nombreFichero, byte[] contenido) {
		this.nombreFichero = nombreFichero;
		this.contenido = contenido;
		this.tamano = contenido.length;
		this.fechaSubida = Instant.now();
	}

}

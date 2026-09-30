package com.alvaro.workflow.moderacion;

import java.time.Instant;
import java.util.UUID;

import com.alvaro.workflow.oferta.Oferta;
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

/** Aviso de un usuario de que una oferta incumple las normas, para que la revise un administrador. */
@Entity
@Table(name = "denuncias")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Denuncia {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "oferta_id", nullable = false)
	private Oferta oferta;

	/** null si quien denunció ha borrado su cuenta. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "denunciante_id")
	private Usuario denunciante;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MotivoDenuncia motivo;

	@Column(columnDefinition = "text")
	private String detalle;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoDenuncia estado;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private Instant fechaCreacion;

	@Column(name = "fecha_resolucion")
	private Instant fechaResolucion;

	Denuncia(Oferta oferta, Usuario denunciante, MotivoDenuncia motivo, String detalle) {
		this.oferta = oferta;
		this.denunciante = denunciante;
		this.motivo = motivo;
		this.detalle = detalle;
		this.estado = EstadoDenuncia.PENDIENTE;
		this.fechaCreacion = Instant.now();
	}

	boolean estaPendiente() {
		return estado == EstadoDenuncia.PENDIENTE;
	}

	void resolver(EstadoDenuncia resultado) {
		this.estado = resultado;
		this.fechaResolucion = Instant.now();
	}

}

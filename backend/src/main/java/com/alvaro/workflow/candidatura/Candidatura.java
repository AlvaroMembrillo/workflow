package com.alvaro.workflow.candidatura;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "candidaturas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Candidatura {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "oferta_id", nullable = false)
	private Oferta oferta;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "candidato_id", nullable = false)
	private Usuario candidato;

	@Column(name = "carta_presentacion", columnDefinition = "text")
	private String cartaPresentacion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoCandidatura estado;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private Instant fechaCreacion;

	@Column(name = "fecha_actualizacion", nullable = false)
	private Instant fechaActualizacion;

	public Candidatura(Oferta oferta, Usuario candidato, String cartaPresentacion) {
		this.oferta = oferta;
		this.candidato = candidato;
		this.cartaPresentacion = cartaPresentacion;
		this.estado = EstadoCandidatura.PENDIENTE;
		this.fechaCreacion = Instant.now();
		this.fechaActualizacion = this.fechaCreacion;
	}

	public void cambiarEstado(EstadoCandidatura nuevo) {
		if (!estado.puedeCambiarA(nuevo)) {
			throw new CambioDeEstadoNoPermitidoException(estado, nuevo);
		}
		this.estado = nuevo;
	}

	@PreUpdate
	void alActualizar() {
		this.fechaActualizacion = Instant.now();
	}

}

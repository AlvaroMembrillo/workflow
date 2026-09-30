package com.alvaro.workflow.oferta;

import java.time.Instant;
import java.util.UUID;

import com.alvaro.workflow.empresa.Empresa;

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
@Table(name = "ofertas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Oferta {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@Column(nullable = false)
	private String titulo;

	@Column(nullable = false, columnDefinition = "text")
	private String descripcion;

	@Column(nullable = false)
	private String ubicacion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Modalidad modalidad;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_contrato", nullable = false)
	private TipoContrato tipoContrato;

	/** Salario bruto anual en euros. */
	@Column(name = "salario_minimo", nullable = false)
	private Integer salarioMinimo;

	@Column(name = "salario_maximo", nullable = false)
	private Integer salarioMaximo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoOferta estado;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private Instant fechaCreacion;

	@Column(name = "fecha_actualizacion", nullable = false)
	private Instant fechaActualizacion;

	public Oferta(Empresa empresa, DatosOferta datos) {
		this.empresa = empresa;
		this.estado = EstadoOferta.ABIERTA;
		this.fechaCreacion = Instant.now();
		this.fechaActualizacion = this.fechaCreacion;
		actualizar(datos);
	}

	public void actualizar(DatosOferta datos) {
		comprobarQueNoEstaRetirada();
		this.titulo = datos.titulo();
		this.descripcion = datos.descripcion();
		this.ubicacion = datos.ubicacion();
		this.modalidad = datos.modalidad();
		this.tipoContrato = datos.tipoContrato();
		this.salarioMinimo = datos.salarioMinimo();
		this.salarioMaximo = datos.salarioMaximo();
	}

	/** La empresa abre o cierra su oferta. */
	public void cambiarEstado(EstadoOferta estado) {
		comprobarQueNoEstaRetirada();
		this.estado = estado;
	}

	/** Moderación retira la oferta. No tiene vuelta atrás para la empresa. */
	public void retirar() {
		this.estado = EstadoOferta.RETIRADA;
	}

	public boolean estaRetirada() {
		return estado == EstadoOferta.RETIRADA;
	}

	private void comprobarQueNoEstaRetirada() {
		if (estaRetirada()) {
			throw new OfertaRetiradaException();
		}
	}

	public boolean estaAbierta() {
		return estado == EstadoOferta.ABIERTA;
	}

	public boolean esDeLaEmpresaDe(UUID usuarioId) {
		return empresa.perteneceA(usuarioId);
	}

	@PreUpdate
	void alActualizar() {
		this.fechaActualizacion = Instant.now();
	}

	/** Campos que la empresa rellena al publicar o editar una oferta. */
	public record DatosOferta(String titulo, String descripcion, String ubicacion, Modalidad modalidad,
			TipoContrato tipoContrato, Integer salarioMinimo, Integer salarioMaximo) {
	}

}

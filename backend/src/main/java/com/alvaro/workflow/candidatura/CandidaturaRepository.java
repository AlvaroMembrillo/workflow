package com.alvaro.workflow.candidatura;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CandidaturaRepository extends JpaRepository<Candidatura, UUID> {

	boolean existsByOfertaIdAndCandidatoId(UUID ofertaId, UUID candidatoId);

	@EntityGraph(attributePaths = { "oferta", "oferta.empresa" })
	Optional<Candidatura> findByOfertaIdAndCandidatoId(UUID ofertaId, UUID candidatoId);

	/** Las candidaturas de un candidato, con su oferta y la empresa de cada oferta en la misma consulta. */
	@EntityGraph(attributePaths = { "oferta", "oferta.empresa" })
	Page<Candidatura> findByCandidatoId(UUID candidatoId, Pageable pageable);

	/** Las candidaturas recibidas en una oferta, con los datos de cada candidato en la misma consulta. */
	@EntityGraph(attributePaths = "candidato")
	Page<Candidatura> findByOfertaId(UUID ofertaId, Pageable pageable);

	@EntityGraph(attributePaths = "candidato")
	Page<Candidatura> findByOfertaIdAndEstadoIn(UUID ofertaId, Collection<EstadoCandidatura> estados, Pageable pageable);

	/** Para cambiar el estado se comprueba la empresa de la oferta y se devuelven los datos del candidato. */
	@Override
	@EntityGraph(attributePaths = { "oferta", "oferta.empresa", "candidato" })
	Optional<Candidatura> findById(UUID id);

	/** Número de candidaturas en cada estado para varias ofertas, en una sola consulta. */
	@Query("""
			select c.oferta.id as ofertaId, c.estado as estado, count(c) as total
			from Candidatura c
			where c.oferta.id in :ofertaIds
			group by c.oferta.id, c.estado
			""")
	List<RecuentoPorEstado> contarPorEstado(Collection<UUID> ofertaIds);

	interface RecuentoPorEstado {

		UUID getOfertaId();

		EstadoCandidatura getEstado();

		long getTotal();

	}

}

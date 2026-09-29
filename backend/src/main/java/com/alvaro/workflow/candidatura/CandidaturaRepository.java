package com.alvaro.workflow.candidatura;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidaturaRepository extends JpaRepository<Candidatura, UUID> {

	boolean existsByOfertaIdAndCandidatoId(UUID ofertaId, UUID candidatoId);

	/** Las candidaturas de un candidato, con su oferta y la empresa de cada oferta en la misma consulta. */
	@EntityGraph(attributePaths = { "oferta", "oferta.empresa" })
	Page<Candidatura> findByCandidatoId(UUID candidatoId, Pageable pageable);

	/** Las candidaturas recibidas en una oferta, con los datos de cada candidato en la misma consulta. */
	@EntityGraph(attributePaths = "candidato")
	Page<Candidatura> findByOfertaId(UUID ofertaId, Pageable pageable);

	/** Para cambiar el estado se comprueba la empresa de la oferta y se devuelven los datos del candidato. */
	@Override
	@EntityGraph(attributePaths = { "oferta", "oferta.empresa", "candidato" })
	Optional<Candidatura> findById(UUID id);

}

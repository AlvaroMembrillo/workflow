package com.alvaro.workflow.oferta;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OfertaRepository extends JpaRepository<Oferta, UUID>, JpaSpecificationExecutor<Oferta> {

	/** Trae la empresa en la misma consulta para no hacer una consulta extra por cada oferta (N+1). */
	@Override
	@EntityGraph(attributePaths = "empresa")
	Page<Oferta> findAll(Specification<Oferta> filtro, Pageable pageable);

	@EntityGraph(attributePaths = "empresa")
	Page<Oferta> findByEmpresaId(UUID empresaId, Pageable pageable);

}

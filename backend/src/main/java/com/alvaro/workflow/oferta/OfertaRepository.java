package com.alvaro.workflow.oferta;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OfertaRepository extends JpaRepository<Oferta, UUID>, JpaSpecificationExecutor<Oferta> {

	/** Trae la empresa en la misma consulta para no hacer una consulta extra por cada oferta (N+1). */
	@Override
	@EntityGraph(attributePaths = "empresa")
	Page<Oferta> findAll(Specification<Oferta> filtro, Pageable pageable);

	@EntityGraph(attributePaths = "empresa")
	Page<Oferta> findByEmpresaId(UUID empresaId, Pageable pageable);

	/** Casi siempre se necesita la empresa (para la respuesta o para comprobar la propiedad de la oferta). */
	@Override
	@EntityGraph(attributePaths = "empresa")
	Optional<Oferta> findById(UUID id);

	/** Cierra en una sola sentencia las ofertas abiertas que no se han actualizado desde {@code limite}. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update Oferta o
			set o.estado = com.alvaro.workflow.oferta.EstadoOferta.CERRADA, o.fechaActualizacion = :ahora
			where o.estado = com.alvaro.workflow.oferta.EstadoOferta.ABIERTA and o.fechaActualizacion < :limite
			""")
	int cerrarSinActividadDesde(Instant limite, Instant ahora);

}

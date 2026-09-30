package com.alvaro.workflow.moderacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface DenunciaRepository extends JpaRepository<Denuncia, UUID> {

	boolean existsByOfertaIdAndDenuncianteId(UUID ofertaId, UUID denuncianteId);

	/** Con la oferta, su empresa, la cuenta de la empresa y quien denunció, para el panel de moderación. */
	@EntityGraph(attributePaths = { "oferta", "oferta.empresa", "oferta.empresa.usuario", "denunciante" })
	Page<Denuncia> findByEstado(EstadoDenuncia estado, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = { "oferta", "oferta.empresa", "oferta.empresa.usuario", "denunciante" })
	Optional<Denuncia> findById(UUID id);

	List<Denuncia> findByOfertaIdAndEstado(UUID ofertaId, EstadoDenuncia estado);

	List<Denuncia> findByOfertaEmpresaIdAndEstado(UUID empresaId, EstadoDenuncia estado);

	/** Las denuncias que ha hecho un usuario, para la copia de sus datos. */
	@EntityGraph(attributePaths = "oferta")
	List<Denuncia> findByDenuncianteIdOrderByFechaCreacionDesc(UUID denuncianteId);

}
